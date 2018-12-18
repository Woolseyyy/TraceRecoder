# -*- coding=utf-8 -*-
"""
Module:     ONE_SEGMENT
Summary:    识别一段出行的交通方式
Author:     Yuhao Jiang
"""
import pandas as pd
import numpy as np
import pickle
from geopy.distance import vincenty
import math
import datetime as dt
import sys
import json

# 载入模型
model = open(r'recognition/transport_classifier.pkl', 'rb')

Walk_Velocity_Threshold = 2.5
Walk_Acceleration_Threshold = 1.5
Time_Threshold = 20
Distance_Threshold = 30

# 计算特征的参数
Average_Walk_Velocity = 1.388
Low_Threshold_Percentage = 0.15
Change_Velocity_Rate_Threshold = 5
Change_Bearing_Rate_Threshold = 30

Low_Threshold = Average_Walk_Velocity * Low_Threshold_Percentage


class OneSegment(object):
    def __init__(self, raw_data):
        self.raw_data = raw_data
        self.data = self.json2pandas()
        self.rf = pickle.load(model)

    @staticmethod
    def calc_distance(pointA, pointB):
        """
        计算A，B点距离
        :param pointA:
        :param pointB:
        :return:
        """
        return vincenty(pointA, pointB).meters

    @staticmethod
    def calc_timestamp(date_time):
        """
        计算时间戳
        :param date_time:
        :return:
        """
        date = dt.datetime.strptime(date_time, '%Y-%m-%d %H:%M:%S')
        timestamp = (date - dt.datetime(1970, 1, 1)).total_seconds()
        return timestamp

    def add_timestamp(self):
        self.data['timestamp'] = list(map(self.calc_timestamp, self.data['date'] + ' ' + self.data['time']))

    @staticmethod
    def calculate_initial_compass_bearing(pointA, pointB):
        """
        计算两个点的方位角
        :param pointA: tuple(latitude, longitude) (39.894178, 116.3182)
        :param pointB: tuple(latitude, longitude) (39.894505, 116.321132)
        :return: compass_bearing                  81.72820612688776
        """

        if (type(pointA) != tuple) or (type(pointB) != tuple):
            raise TypeError('Only tuples are supported as argument')

        lat1 = math.radians(pointA[0])
        lat2 = math.radians(pointB[0])

        diffLong = math.radians(pointB[1] - pointA[1])

        # 方位角（-PI to PI)
        # θ = atan2(sin(Δlong)*cos(lat2),cos(lat1)*sin(lat2) − sin(lat1)*cos(lat2)*cos(Δlong))
        x = math.sin(diffLong) * math.cos(lat2)
        y = math.cos(lat1) * math.sin(lat2) - (math.sin(lat1) * math.cos(lat2) * math.cos(diffLong))

        initial_bearing = math.atan2(x, y)

        # 标准方位角（0 to 360 degrees)
        initial_bearing = math.degrees(initial_bearing)
        compass_bearing = (initial_bearing + 360) % 360

        return compass_bearing

    def json2pandas(self):
        """
        把输入的list数据转化成pandas数据
        :return:
        """
        segment_ids = []
        trans_modes = []
        former_trans_modes = []
        latitudes = []
        longitudes = []
        dates = []
        times = []

        for i in range(0, len(self.raw_data)):
            row_i = self.raw_data[i]
            for j in range(0, len(row_i['data'])):
                row_j = self.raw_data[i]['data'][j]
                segment_ids.append(row_i['segment_ID'])
                trans_modes.append(row_i['trans_mode'])
                former_trans_modes.append(row_i['former_trans_mode'])
                latitudes.append(float(row_j['latitude']))
                longitudes.append(float(row_j['longitude']))
                dates.append(row_j['date'])
                times.append(row_j['time'])

        d = {
            'segment_ID': segment_ids,
            'trans_mode': trans_modes,
            'former_trans_mode': former_trans_modes,
            'latitude': latitudes,
            'longitude': longitudes,
            'date': dates,
            'time': times
        }
        # columns = ['segment_ID', 'trans_mode', 'former_trans_mode', 'latitude', 'longitude', 'date', 'time']
        # array = np.array([segment_ids, trans_modes, former_trans_modes, latitudes, longitudes, dates, times]).transpose()
        # data = pd.DataFrame(array, columns=columns)
        data = pd.DataFrame(d)
        return data

    def calc_gps_feature(self):
        """
        计算速度，加速度
        :param
        :return:
        """
        # 增加新的列
        self.data['time_delta'] = 0
        self.data['distance_delta'] = 0
        self.data['velocity'] = 0
        self.data['velocity_ratio'] = 0
        self.data['acceleration'] = 0
        self.data['acceleration_ratio'] = 0
        self.data['bearing_delta'] = 0
        self.data['bearing_delta_redirect'] = 0
        self.add_timestamp()

        distance_delta = []
        time_delta = []
        velocity = []
        velocity_ratio = []
        acceleration = []
        acceleration_ratio = []
        bearing_delta = []
        bearing_delta_redirect = []

        # 上一个点的数据
        pre_latitude = 0
        pre_longitude = 0
        pre_time = 0
        pre_velocity = 0
        pre_acceleration = 0
        pre_bearing = 0

        for i, row in self.data.iterrows():
            if i == 0:
                pre_latitude = row['latitude']
                pre_longitude = row['longitude']
                distance_delta.append(0)

                pre_time = row['timestamp']
                time_delta.append(0)

                pre_velocity = 0
                velocity.append(0)
                velocity_ratio.append(0)

                pre_acceleration = 0
                acceleration.append(0)
                acceleration_ratio.append(0)

                pre_bearing = 0
                bearing_delta.append(0)
                bearing_delta_redirect.append(0)

                continue

            # 1.计算时间间隔
            t_delta = row['timestamp'] - pre_time
            # 如果两个GPS有相同时间戳，则设置为间隔1s
            if t_delta == 0:
                t_delta = 1

            time_delta.append(t_delta)

            # 2.计算距离
            pointA = (pre_latitude, pre_longitude)
            pointB = (row['latitude'], row['longitude'])

            d_delta = self.calc_distance(pointA, pointB)
            distance_delta.append(d_delta)

            # 3.计算速度
            v = d_delta / float(t_delta)
            velocity.append(v)

            # 4.计算速度变化率，在加速度里再考虑角速度的符号。这里使用绝对值
            if pre_velocity != 0:
                v_ratio = abs(v - pre_velocity) / float(pre_velocity)
            else:
                v_ratio = 0

            velocity_ratio.append(v_ratio)

            # 5.计算加速度
            acc = (v - pre_velocity) / float(t_delta)
            acceleration.append(acc)

            # 6.计算加速度变化率
            if pre_acceleration != 0:
                acc_ratio = abs((acc - pre_acceleration) / float(pre_acceleration))
            else:
                acc_ratio = 0

            acceleration_ratio.append(acc_ratio)

            # 7.计算方位角差
            bear_delta = self.calculate_initial_compass_bearing(pointA, pointB)

            bearing_delta.append(bear_delta)

            # 8.计算方位角差变化率
            if pre_bearing != 0:
                bear_delta_ratio = abs(bear_delta - pre_bearing)
            else:
                bear_delta_ratio = 0

            bearing_delta_redirect.append(bear_delta_ratio)

            # 设置当前的参数，用作下一次循环用
            pre_latitude = row['latitude']
            pre_longitude = row['longitude']
            pre_time = row['timestamp']
            pre_velocity = v
            pre_acceleration = acc
            pre_bearing = bear_delta

        self.data.loc[:, 'time_delta'] = time_delta
        self.data.loc[:, 'distance_delta'] = distance_delta
        self.data.loc[:, 'velocity'] = velocity
        self.data.loc[:, 'velocity_ratio'] = velocity_ratio
        self.data.loc[:, 'acceleration'] = acceleration
        self.data.loc[:, 'acceleration_ratio'] = acceleration_ratio
        self.data.loc[:, 'bearing_delta'] = bearing_delta
        self.data.loc[:, 'bearing_delta_redirect'] = bearing_delta_redirect

    def calc_segment_feature(self):
        seg_columns = ['segment_ID',                         # Segment_ID
                       'former_trans_mode',                  # 前一段的交通工具
                       'time_total',                         # 一段segment的时间
                       'distance_total',                     # 一段segment的距离
                       'velocity_mean_distance',             # distance_total / time_total
                       'velocity_mean_segment',              # 所有速度的和/轨迹点数
                       'velocity_top1',                      # 第一速度
                       'velocity_top2',                      # 第二速度
                       'velocity_top3',                      # 第三速度
                       'acceleration_top1',                  # 第一加速度
                       'acceleration_top2',                  # 第二加速度
                       'acceleration_top3',                  # 第三加速度
                       'velocity_low_rate_distance',         # 低于阈值的速度 / distance_total
                       'velocity_change_rate',               # 速度变化超过阈值的数量 / distance_total
                       'bearing_change_rate'                 # 方位角变化超过阈值的数量 / distance_total
                       ]

        df_seg = pd.DataFrame(columns=seg_columns)
        df_seg = df_seg.fillna(0)

        # segment特征值
        segment_ID = 0
        former_trans_mode = 0
        time_total = 0
        distance_total = 0
        velocity_mean_distance = 0
        velocity_mean_segment = 0
        velocity_top1 = 0
        velocity_top2 = 0
        velocity_top3 = 0
        acceleration_top1 = 0
        acceleration_top2 = 0
        acceleration_top3 = 0
        velocity_low_rate_distance = 0
        velocity_change_rate = 0
        bearing_change_rate = 0

        segment_ID = self.data.segment_ID[0]

        # 总时间
        time_total = np.sum(self.data.time_delta)
        if time_total == 0:
            time_total = 1
        # print('time_total: ', time_total)

        # 总距离
        distance_total = np.sum(self.data.distance_delta)
        if distance_total == 0:
            distance_total = 1
        # print('distance_total: ', distance_total)

        # 平均速度——时间&距离
        velocity_mean_distance = distance_total / time_total

        # 平均速度——数量
        velocity_mean_segment = np.sum(self.data.velocity) / len(self.data)

        # 最大速度
        velocity_copy = self.data.velocity.copy()
        topvelocity = sorted(velocity_copy, reverse=True)

        # Top1
        velocity_top1 = topvelocity[0]

        # Top2 & Top3
        if len(topvelocity) >= 2:
            velocity_top2 = topvelocity[1]
        if len(topvelocity) >= 3:
            velocity_top3 = topvelocity[2]

        # 最大加速度
        acceleration_copy = self.data.acceleration.copy()
        topacceleration = sorted(acceleration_copy, reverse=True)

        # Top1
        acceleration_top1 = topacceleration[0]

        # Top2&Top3
        if len(topacceleration) >= 2:
            acceleration_top2 = topacceleration[1]

        if len(topacceleration) >= 3:
            acceleration_top3 = topacceleration[2]

        # 低速度的比例
        velocity_low_rate_distance = self.data.velocity[self.data.velocity < Low_Threshold].count() / distance_total

        # 速度变化值高的比例
        velocity_change_rate = self.data.velocity_ratio[self.data.velocity_ratio >
                                                        Change_Velocity_Rate_Threshold].count() / distance_total
        # 方位角变化大的比例
        bearing_change_rate = self.data.bearing_delta_redirect[self.data.bearing_delta_redirect >
                                                               Change_Bearing_Rate_Threshold].count() / distance_total

        former_trans_mode = None
        # 存储数据
        df_temp = pd.DataFrame([[segment_ID,
                                 former_trans_mode,
                                 time_total,
                                 distance_total,
                                 velocity_mean_distance,
                                 velocity_mean_segment,
                                 velocity_top1,
                                 velocity_top2,
                                 velocity_top3,
                                 acceleration_top1,
                                 acceleration_top2,
                                 acceleration_top3,
                                 velocity_low_rate_distance,
                                 velocity_change_rate,
                                 bearing_change_rate]], columns=seg_columns)
        return df_temp

    def recognition(self):
        df_temp = self.calc_segment_feature()
        x = df_temp.loc[:, 'time_total':'bearing_change_rate']
        y_predict = self.rf.predict(x)
        trans_modes = ['bike', 'bus', 'car', 'walk']
        trans_mode = trans_modes[int(y_predict)-1]
        print(trans_mode)


if __name__ == '__main__':
    str_data = sys.argv[1]
    json_data = json.loads(str_data)
    og = OneSegment(json_data['data'])
    og.calc_gps_feature()
    og.recognition()
