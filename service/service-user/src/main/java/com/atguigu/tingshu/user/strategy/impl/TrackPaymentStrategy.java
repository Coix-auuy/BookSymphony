package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.strategy.PaymentStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Author HeZx
 * Time 2024/12/21 16:40
 * Version 1.0
 * Description: 购买声音
 */
@Service(SystemConstant.ORDER_ITEM_TYPE_TRACK)
public class TrackPaymentStrategy implements PaymentStrategy {
    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Override
    public void processPayment(UserPaidRecordVo userPaidRecordVo) {
        // 声音
        Long count = userPaidTrackMapper.selectCount(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo()));
        if (count != 0) {
            return;
        }
        // 保存数据
        // 远程调用获取专辑 Id
        Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfo(userPaidRecordVo.getItemIdList().get(0));
        Assert.notNull(trackInfoResult, "声音信息结果集为空");
        TrackInfo trackInfo = trackInfoResult.getData();
        Assert.notNull(trackInfo, "声音信息为空");
        userPaidRecordVo.getItemIdList().forEach(trackId -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidTrack.setAlbumId(trackInfo.getAlbumId());
            userPaidTrack.setTrackId(trackId);
            userPaidTrackMapper.insert(userPaidTrack);
        });
    }
}
