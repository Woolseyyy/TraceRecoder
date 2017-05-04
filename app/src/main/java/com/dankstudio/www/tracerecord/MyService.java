package com.dankstudio.www.tracerecord;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.*;
import com.baidu.trace.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.*;

public class MyService extends Service {
    private Trip trip;
    private User user;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient okHttpClient = new OkHttpClient();

    //轨迹参数
    private long serviceId  = 138084;//鹰眼服务ID
    private String entityName = null;//entity标识
    private int  traceType = 2;//轨迹服务类型（0 : 不上传位置数据，也不接收报警信息； 1 : 不上传位置数据，但接收报警信息；2 : 上传位置数据，且接收报警信息）
    private int gatherInterval = 3;// 采集周期
    private int packInterval = 10;// 打包周期
    private int protocolType = 1;// http协议类型


    //实时查询轨迹参数
    int simpleReturn = 0;// 是否返回精简结果
    int isProcessed = 0; // 是否纠偏
    String processOption = null;// 纠偏选项
    int startTime = (int)(System.currentTimeMillis()/1000);;// 开始时间
    int pageSize = 5000;// 分页大小
    int pageIndex = 1;// 分页索引

    //map draw
    BaiduMap mBaiduMap;
    int[] colors = new int[]{Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW};
    int colorNum = 4;
    int colorIndex = 0;

    private Trace trace;//实例化轨迹服务
    private LBSTraceClient client;//实例化轨迹服务客户端


    //实例化开启轨迹服务回调接口
    private OnStartTraceListener  startTraceListener = new OnStartTraceListener() {
        //开启轨迹服务回调接口（arg0 : 消息编码，arg1 : 消息内容，详情查看类参考）
        @Override
        public void onTraceCallback(int arg0, String arg1) {
        }
        //轨迹服务推送接口（用于接收服务端推送消息，arg0 : 消息类型，arg1 : 消息内容，详情查看类参考）
        @Override
        public void onTracePushCallback(byte arg0, String arg1) {
        }
    };

    //实例化停止轨迹服务回调接口
    private OnStopTraceListener stopTraceListener = new OnStopTraceListener(){
        // 轨迹服务停止成功
        @Override
        public void onStopTraceSuccess() {
        }
        // 轨迹服务停止失败（arg0 : 错误编码，arg1 : 消息内容，详情查看类参考）
        @Override
        public void onStopTraceFailed(int arg0, String arg1) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        entityName = getImei(getApplicationContext());  //手机Imei值的获取，用来充当实体名

        traceInit();

        startTime = (int)(System.currentTimeMillis()/1000);// 开始时间
    }

    //init
    private void traceInit(){
        client = new LBSTraceClient(getApplicationContext()); //实例化轨迹客户端
        trace = new Trace(getApplicationContext(), serviceId, entityName, traceType);//实例化轨迹服务

        client. setInterval(gatherInterval, packInterval);// 设置采集和打包周期
        client. setLocationMode(LocationMode.High_Accuracy); // 设置定位模式
        client. setProtocolType (protocolType); // 设置http协议类型
    }
    public void dataUpdate(final MainActivity activity){
        try {
            get("http://123.206.108.55:3000/user?id=" + entityName,
                    new Callback()
                    {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e("MY", "request error!");
                            user = new User("", entityName, 0);
                            e.printStackTrace();
                            activity.btnStatue = 0;
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String result = response.body().string();
                            JSONObject jsonUser = null;
                            String areaID ="";
                            String id =entityName;
                            int maxTripNum = 0;
                            try {
                                jsonUser = new JSONObject(result).getJSONObject("body");
                                areaID = jsonUser.getString("areaID");
                                id = jsonUser.getString("id");
                                maxTripNum = jsonUser.getInt("maxTripNum");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            user = new User(areaID, id, maxTripNum);
                            activity.btnStatue = 0;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String hint(){
        return trip.hint();
    }

    public boolean ifParking(){return trip.ifParking();}

    //connect using binder
    private final IBinder binder = new MyBinder();
    class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    void post(String url, String json) throws IOException {
        Log.e("MY", url);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        //new call
        Call call =okHttpClient.newCall(request);
        //请求加入调度
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MY", "send failed!"+e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("MY", "send success!"+response);
            }
        });
    }

    void get(String url, Callback cb) throws IOException {
        Request request = new Request.Builder().url(url).build();
        //new call
        Call call =okHttpClient.newCall(request);
        //请求加入调度
        call.enqueue(cb);
    }



    //api
    public void modeChange(TravelWay way, ParkingPlace place){
        Log.e("MY", "enter modeChange:"+way);
        trip.modeChange(way, place);
        colorChange();
    }

    private void colorChange(){
        colorIndex = (colorIndex+1)%colorNum;
    }

    private int getColor(){
        return colors[colorIndex];
    }

    public void start(TravelPurpose purpose, TravelWay way){
        client.startTrace(trace, startTraceListener);//开启轨迹服务

        //new trip
        Tools tools = new Tools(client, serviceId, entityName);
        trip = new Trip(user, purpose, tools, way);

    }
    public void stop(ParkingPlace place){
        trip.stopTrip(place);
        client.stopTrace(trace,stopTraceListener); //停止轨迹服务
    }

    public void sendToServicer(){
        //send trip and change the user
        JSONObject tripJson = trip.toJson();
        JSONObject userJson = user.toJson();
        JSONObject body = new JSONObject();

        try {
            body.put("user", userJson);
            body.put("trip", tripJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //connect
        try {
            post("http://123.206.108.55:3000/save", body.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void setMap(BaiduMap in){
        mBaiduMap = in;
    }

    /*private OnTrackListener trackListenerR = new OnTrackListener() {
        public void onQueryHistoryTrackCallback(String message) {
            Log.e("MY", "Get Track!");
            JSONObject data = null;
            try {
                data = new JSONObject(message);
                JSONObject endPoint = data. getJSONObject ("end_point");
                //更新startTime，在当前查询的最后一个点的时间戳上加1，作为下一次查询的开始时间
                startTime = endPoint.getInt("loc_time") + 1;

                List<LatLng> pointsD = new ArrayList<LatLng>();
                JSONArray points = data.getJSONArray("points");
                for(int i=0; i<points.length(); i++){
                    JSONObject temp = points.getJSONObject(i);
                    JSONArray location = temp.getJSONArray("location");
                    pointsD.add(new LatLng(
                            location.getDouble(0),
                            location.getDouble(1)
                    ));
                }
                PolylineOptions options = new PolylineOptions().color(getColor()).points(pointsD).width(15);
                mBaiduMap.addOverlay(options);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRequestFailedCallback(String s) {
            Log.e("MY", "request trace failed!");
        }
    };*/

    public void queryHistoryTrack() {
        // 结束时间
        int endTime = (int)(System.currentTimeMillis()/1000);
        // 查询新增的轨迹
        //client.queryHistoryTrack(serviceId , entityName, simpleReturn, isProcessed,
        //        processOption, startTime, endTime, pageSize, pageIndex, trackListenerR);
        client.queryHistoryTrack(serviceId , entityName, simpleReturn, isProcessed,
                processOption, startTime, endTime, pageSize, pageIndex,
                new OnTrackListener() {
                    public void onQueryHistoryTrackCallback(String message) {
                        Log.e("MY", "Get Track!");
                        JSONObject data = null;
                        try {
                            data = new JSONObject(message);
                            JSONObject endPoint = data. getJSONObject ("end_point");
                            //更新startTime，在当前查询的最后一个点的时间戳上加1，作为下一次查询的开始时间
                            startTime = endPoint.getInt("loc_time") + 1;

                            List<LatLng> pointsD = new ArrayList<LatLng>();
                            JSONArray points = data.getJSONArray("points");
                            for(int i=0; i<points.length(); i++){
                                JSONObject temp = points.getJSONObject(i);
                                JSONArray location = temp.getJSONArray("location");
                                pointsD.add(new LatLng(
                                        location.getDouble(0),
                                        location.getDouble(1)
                                ));
                            }
                            PolylineOptions options = new PolylineOptions().color(getColor()).points(pointsD).width(15);
                            mBaiduMap.addOverlay(options);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onRequestFailedCallback(String s) {
                        Log.e("MY", "request trace failed!");
                    }
                });
    }


    //获取手机的Imei码，作为实体对象的标记值
    private String getImei(Context context){
        String mImei = "NULL";
        try {
            mImei = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        } catch (Exception e) {
            System.out.println("获取IMEI码失败");
            mImei = "NULL";
        }
        return mImei;
    }
}

class Tools{
    private ReverseGeoCodeOption myReverseGeoCodeOption = new ReverseGeoCodeOption();
    public ReverseGeoCodeResult myReverseGeoCodeResult;
    public ReverseGeoCodeResult.AddressComponent myAddressComponent;
    private LBSTraceClient client;
    private long serviceId;
    private String entityName;

    Tools(LBSTraceClient in_client, long in_serviceId, String in_entityName){
        client = in_client;
        serviceId = in_serviceId;
        entityName = in_entityName;
    }

    class LocTools{
        //private String loc;
        //private double[] locData;
        private MyLocation loc;
        private GeoCoder mSearch = GeoCoder.newInstance();

        LocTools(MyLocation in_loc){
            loc = in_loc;
            mSearch.setOnGetGeoCodeResultListener(geoCoderListener);
        }
        protected void finalize(){
            mSearch.destroy();
        }

        private OnEntityListener entityListener = new OnEntityListener() {
            @Override
            public void onRequestFailedCallback(String s) {

            }

            public void onReceiveLocation(TraceLocation location){
                Log.e("MY", "-1:" + location.toString());
                Log.e("MY", "0:"+location.getLatitude()+ ","+ location.getLongitude());
                Log.e("MY", "locListenr:"+this);
                //发起反编码查询
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                myReverseGeoCodeOption.location(latLng);
                mSearch.reverseGeoCode(myReverseGeoCodeOption);
            }
        };

        private OnGetGeoCoderResultListener geoCoderListener = new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有检索到结果
                }
                //获取地理编码结果
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    //没有找到检索结果
                    loc.describe = "无地址信息";
                    loc.data = new double[]{-1, -1};
                    Log.e("MY", loc.describe);
                }
                else{
                    //Log.e("MY", "1:"+result.getAddress());
                    //Log.e("MY", "2:"+result.getSematicDescription());
                    loc.describe =  ( result.getAddress() + result.getSematicDescription() );
                    //Log.e("MY", loc.describe);
                    loc.data = new double[]{result.getLocation().latitude, result.getLocation().longitude};
                    Log.e("MY", loc.describe);
                }
            }
        };

        void start(){
            //Log.e("MY", "-2 client"+ client.toString());
            //Log.e("MY", "-2 serviceID"+ serviceId);
            client.queryRealtimeLoc(serviceId, entityListener);
        }

    }

    void getLoc(MyLocation loc){
        LocTools locTools = new LocTools(loc);
        locTools.start();
    }

    double[] subTime(Date s, Date e){
        long time = e.getTime() - s.getTime();
        time/=1000;
        double[] d = new double[3];
        d[2] = time % 60;
        time/=60;
        d[1] = time % 60;
        time/=60;
        d[0] = time;
        return d;
    }

    class Distance{
        private SubTrip t;
        Distance(SubTrip in){
            t = in;
        }

        private OnTrackListener onTrackListener = new OnTrackListener() {
            @Override
            public void onRequestFailedCallback(String s) {
                Log.e("MY", "distance failed!");
            }

            public void onQueryDistanceCallback(String message){
                try {
                    JSONObject obj = new JSONObject(message);
                    Log.e("MY", ""+obj);
                    t.distance = obj.getDouble("distance");
                    //Log.e("MY", ""+t.distance);
                    //Log.e("MY", "distanceSetT:"+t);
                    //Log.e("MY", "thisListener:"+this);

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        };

        void query(){
            Date s = t.sDate;
            Date e = t.eDate;

            //Log.e("MY", "distanceID"+t.id);
            //Log.e("MY", "listener:" +onTrackListener);


            client.setOnTrackListener(onTrackListener);

            client.queryDistance(serviceId, entityName,
                    1, "need_denoise=1,need_vacuate=1,need_mapmatch=0",
                    "no_supplement",
                    (int)(s.getTime() / 1000), (int)(e.getTime() / 1000),null
            );
        }
    }

    void queryDistanceOFSubTrip(SubTrip t){
        Distance distance = new Distance(t);
        distance.query();
    }

}

class Trip {
    private String userId;
    private int id = -1;
    private Date date = new Date();
    private TravelPurpose purpose;
    private ArrayList<SubTrip> children = new ArrayList<>();
    private MyLocation sLocation = new MyLocation();
    private MyLocation eLocation = new MyLocation();
    //public String sLoc = null;
    //public String eLoc = null;
    //public double[] sLocData = null;
    //public double[] eLocData = null;
    private Date sDate = new Date();
    private Date eDate;
    private int waysCount = 0;
    private double[] deltaTime;
    public double distance = 0;

    private Tools tools;

    String hint(){
        SubTrip subTrip = children.get(children.size()-1);
        double[] time = tools.subTime(subTrip.sDate, new Date());
        String res = "目前所在行程段：" + subTrip.id + "\n"+
                "交通方式：" + subTrip.way.getName() + "\n"+
                "已进行：" + (int)time[0] + " : " + (int)time[1] + " : " + (int)time[2];
        return res;
    }

    public Trip(User user, TravelPurpose pur, Tools in_tools, TravelWay way){
        userId = user.getId();
        id = user.AddTripNum();
        purpose = pur;
        tools = in_tools;
        tools.getLoc(sLocation);

        addSubTrip(way);
    }

    public boolean ifParking(){
        return children.get(children.size()-1).way.ifParking();
    }

    private void addSubTrip(TravelWay way){
        SubTrip child = new SubTrip();
        //init
        child.id = children.size();
        child.way.c = way.c;
        tools.getLoc(child.sLocation);

        //add
        children.add(child);
    }

    private void completeLastSubTrip(ParkingPlace place){
        SubTrip child = children.get(children.size()-1);
        tools.getLoc(child.eLocation);
        child.place.c = place.c;
        child.eDate = new Date();
        child.deltaTime = tools.subTime(child.sDate, child.eDate);
        tools.queryDistanceOFSubTrip(child);
    }

    void modeChange(TravelWay way, ParkingPlace place){
        completeLastSubTrip(place);
        addSubTrip(way);
        Log.e("MY", "mode change complete");
        Log.e("MY", children.toString());
    }

    void stopTrip(ParkingPlace place){
        completeLastSubTrip(place);
        eDate = new Date();
        deltaTime = tools.subTime(sDate, eDate);
        waysCount = countWays();
        //distance = computeDistance();
        tools.getLoc(eLocation);
    }

    private int countWays(){
        HashSet<SubTrip> set = new HashSet<>();
        set.addAll(children);
        return set.size();
    }
    private int computeDistance(){
        int dis=0;
        for (SubTrip child:children) {
            dis+=child.distance;
        }
        return dis;
    }

    public JSONObject toJson(){
        distance = computeDistance();
        JSONObject json = new JSONObject();
        JSONArray jsonDeltaTime = new JSONArray();
        JSONArray jsonSubTrips = new JSONArray();
        for (double num : deltaTime) {
            try {
                jsonDeltaTime.put(num);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        for (SubTrip subTrip : children) {
            jsonSubTrips.put(subTrip.toJson());
        }

        try {
            json.put("id", id);
            json.put("date", date);
            json.put("purpose", purpose.c);
            json.put("sDate", sDate);
            json.put("eDate", eDate);
            json.put("waysCount", waysCount);
            json.put("deltaTime", jsonDeltaTime);
            json.put("sLocation", sLocation.toJson());
            json.put("eLocation", eLocation.toJson());
            json.put("children", jsonSubTrips);
            json.put("distance", distance);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class SubTrip {
    public int id = -1;
    public MyLocation sLocation = new MyLocation();
    public MyLocation eLocation = new MyLocation();
    //public String sLoc = null;
    //public String eLoc = null;
    //public double[] sLocData = null;
    //public double[] eLocData = null;
    public Date sDate = new Date();
    public Date eDate;
    public TravelWay way = new TravelWay();
    public ParkingPlace place = new ParkingPlace();
    public double[] deltaTime;
    public double distance = 0;

    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        JSONArray jsonDeltaTime = new JSONArray();
        for (double num : deltaTime) {
            try {
                jsonDeltaTime.put(num);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            json.put("id", id);
            json.put("sLocation", sLocation.toJson());
            json.put("eLocation", eLocation.toJson());
            json.put("sDate", sDate);
            json.put("eDate", eDate);
            json.put("way", way.c);
            json.put("place", place.c);
            json.put("deltaTime", jsonDeltaTime);
            json.put("distance", distance);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class User{
    private String areaId;
    private String id;
    private int maxTripNum = 0;

    User(String in_areaId, String in_id, int in_maxTripNum){
        areaId = in_areaId;
        id = in_id;
        maxTripNum = in_maxTripNum;
    }

    public String getId(){
        return id;
    }
    public int getMaxTripNum(){
        return maxTripNum;
    }
    int AddTripNum(){
        return maxTripNum++;
    }
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        try {
            json.put("areaId", areaId);
            json.put("id", id);
            json.put("maxTripNum", maxTripNum);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class MyLocation {
    public double[] data = {0, 0, 0};
    public String describe;
    public JSONObject toJson(){
        JSONObject json = new JSONObject();
        JSONArray jsonData = new JSONArray();
        for (double num : data) {
            try {
                jsonData.put(num);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            json.put("data", jsonData);
            json.put("describe", describe);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}



