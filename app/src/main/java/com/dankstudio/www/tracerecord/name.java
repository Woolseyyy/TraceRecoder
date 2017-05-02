package com.dankstudio.www.tracerecord;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by admin on 2017/4/15.
 */
class TravelWay {
    private final String[] ways = {"步行", "自行车", "电动车", "摩托车",
            "公共汽车", "地铁", "公司班车", "小区班车",
            "网约/出租车", "私家车", "公司配车", "货车", "其他"};
    private final int[] id = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

    public int c = 0;

    public int map(String s){
        for(int i=0; i<ways.length; i++){
            if(ways[i].equals(s)){
                return id[i];
            }
        }
        return 0;
    }
    public TravelWay(){
        c = 0;
    }
    public TravelWay(int i) { c = i;}
    public TravelWay(String s){
        c = map(s);
    }
    public void set(String s){
        c = map(s);
    }
    public void set(int t){
        c = t;
    }

}

class ParkingPlace {
    private final String[] ways = {"车库", "地面停车位", "无停车位", "宿舍楼内",
            "实验室", "其他"};
    private final int[] id = {1, 2, 3, 4, 5, 6};

    public int c = 0;

    public int map(String s){
        for(int i=0; i<ways.length; i++){
            if(ways[i].equals(s)){
                return id[i];
            }
        }
        return 0;
    }
    public ParkingPlace(){
        c = 0;
    }
    public ParkingPlace(int i) { c = i;}
    public ParkingPlace(String s){
        c = map(s);
    }
    public void set(String s){
        c = map(s);
    }
    public void set(int t){
        c = t;
    }


}

class TravelPurpose{
    private final String[] ways = {"上班", "上学", "回家", "业务",
            "回单位（学校）", "就餐", "购物", "休闲娱乐",
            "探亲访友", "其他", "接送其他", "就医"};
    private final int[] id = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    public int c = 0;
    public int map(String s){
        for(int i=0; i<ways.length; i++){
            if(ways[i].equals(s)){
                return id[i];
            }
        }
        return 0;
    }
    public TravelPurpose(){
        c = 0;
    }
    public TravelPurpose(int i) { c = i;}
    public TravelPurpose(String s){
        c = map(s);
    }
    public void set(String s){
        c = map(s);
    }
    public void set(int t){
        c = t;
    }
}


