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
import com.baidu.mapapi.map.MapView;

public class MainActivity extends AppCompatActivity {
    //layout item
    MapView mMapView = null;
    Button btn_start;
    Button btn_modeChange;

    //辅助参数
    boolean ifStart = false;
    int wayMenuPurpose = 0;//0:无目的 1:start 2:change mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //service connect
        bindServiceConnection();

        //sdk init
        SDKInitializer.initialize(getApplicationContext());

        //layout connect
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.bmapView);
        btn_start = (Button) findViewById(R.id.switcher);
        btn_modeChange = (Button) findViewById(R.id.mode);

        //listener
        SetListener();
    }

    private void SetListener(){
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!ifStart){
                    wayMenuPurpose = 1;
                    showPurpose(view);

                    //after choose way:
                    //btn_start.setText("Stop");
                    //ifStart = !ifStart;


                }
                else{
                    myService.stop();
                    btn_start.setText("Start");
                    ifStart = !ifStart;
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
                    ifStart = !ifStart;
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
