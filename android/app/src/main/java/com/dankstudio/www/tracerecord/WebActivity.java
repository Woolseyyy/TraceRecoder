package com.dankstudio.www.tracerecord;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebActivity extends AppCompatActivity {

    private WebView webView;
    private String entityName = null;//entity标识

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        entityName = getImei(getApplicationContext());
        webView = (WebView) findViewById(R.id.web);
        webviewConfig();
        webView.loadUrl("http://123.206.108.55:3000/info?id="+entityName);
    }

    private void webviewConfig(){
        //声明WebSettings子类
        WebSettings webSettings = webView.getSettings();

        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);

        //设置自适应屏幕，两者合用
        //webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        //webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        //缩放操作
        //webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        //webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        //webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        //其他细节操作
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //关闭webview中缓存
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
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
