package com.dankstudio.www.tracerecord;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;

public class MainActivity extends AppCompatActivity {
    //layout item
    MapView mMapView = null;
    Button btn_start;
    Button btn_modeChange;
    Button btn_info;

    BaiduMap mBaiduMap;

    //辅助参数
    int btnStatue = -1;
    int wayMenuPurpose = 0;//0:无目的 1:start 2:change mode

    Intent webIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //service connect
        bindServiceConnection();

        webIntent = new Intent(MainActivity.this, WebActivity.class);

        //sdk init
        SDKInitializer.initialize(getApplicationContext());

        //layout connect
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.bmapView);
        btn_start = (Button) findViewById(R.id.switcher);
        btn_modeChange = (Button) findViewById(R.id.mode);
        btn_info = (Button) findViewById(R.id.info);

        //map init
        mBaiduMap = mMapView.getMap();
        //myService.setMap(mBaiduMap);

        //listener
        SetListener();

    }

    class RefreshMap extends Thread{
        public void run(){
            while (true){
                myService.queryHistoryTrack();
            }
        }
    }

    private void SetListener(){

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnStatue==-1){
                    btnStatue=-1;
                    btn_start.setText("连接中");
                    myService.dataUpdate(MainActivity.this);
                    while (btnStatue==-1){}
                    btn_start.setText("START");
                }

                if (btnStatue==0){


                    wayMenuPurpose = 1;
                    showPurpose(view);

                    //after choose way:
                    //btn_start.setText("Stop");
                    //btnStatue = !btnStatue;


                }
                else if(btnStatue==1){
                    myService.stop();
                    btn_start.setText("Send");
                    btnStatue = btnStatue+1;
                    myService.setMap(mBaiduMap);
                    //myService.queryHistoryTrack();
                }
                else if(btnStatue==2){
                    myService.sendToServicer();
                    btn_start.setText("Start");
                    btnStatue = 0;
                }
            }
        });

        btn_modeChange.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wayMenuPurpose = 2;
                showWay(view, null);
            }
        }));

        btn_info.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                startActivity(webIntent);
            }
        });
    }

    private void showPurpose(final View view){
        PopupMenu menu = new PopupMenu(this,view);
        menu.getMenuInflater().inflate(R.menu.purpose,menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                TravelPurpose purpose = new TravelPurpose(item.getTitle().toString());
                showWay(view, purpose);
                return true;
            }
        });
        menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {

            }
        });
        menu.show();
    }

    private void showWay(View view, final TravelPurpose purpose){
        PopupMenu menu = new PopupMenu(this,view);
        menu.getMenuInflater().inflate(R.menu.way,menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                TravelWay way = new TravelWay(item.getTitle().toString());
                if(wayMenuPurpose==1)//start
                {
                    wayMenuPurpose = 0;
                    myService.start(purpose, way);
                    btn_start.setText("Stop");
                    btnStatue = btnStatue+1;

                    RefreshMap refreshMap = new RefreshMap();
                    //refreshMap.start();
                }
                else if(wayMenuPurpose==2)//mode change
                {
                    wayMenuPurpose = 0;
                    myService.modeChange(way);
                }
                return true;
            }
        });
        menu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {

            }
        });
        menu.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override

    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    //service
    private MyService myService;
    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            myService = ((MyService.MyBinder)iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            myService = null;
        }
    };
    private void bindServiceConnection() {
        Intent intent = new Intent(MainActivity.this, MyService.class);
        bindService(intent, sc, this.BIND_AUTO_CREATE);

    }

}
