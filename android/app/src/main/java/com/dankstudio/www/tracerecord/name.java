package com.dankstudio.www.tracerecord;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by admin on 2017/4/15.
 */
class TravelWay {
    private final String[] ways = {"步行", "自行车", "电动车", "校览车",
            "校车", "小汽车", "公交车", "摩托车",
            "网约/出租车", "其他"};
    private final int[] id = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

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
    public String getName(){ return  (c>0)?ways[c-1]:"无方式";}
    boolean ifParking(){return (c==2 || c==3 || c==8);}

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
    private final String[] ways = {"办事", "上课", "就餐", "自习", "购物",
            "回寝室", "社团活动", "学术活动(如讲座)",
            "娱乐活动", "就医", "其他"};
    private final int[] id = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

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
