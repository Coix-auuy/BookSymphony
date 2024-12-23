package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.strategy.PaymentStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Author HeZx
 * Time 2024/12/21 16:37
 * Version 1.0
 * Description: 购买专辑
 */
@Service(SystemConstant.ORDER_ITEM_TYPE_ALBUM)
public class AlbumPaymentStrategy implements PaymentStrategy {
    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void processPayment(UserPaidRecordVo userPaidRecordVo) {
        // 先查询这个用户记录表，是否已经存储数据了，防止重复消费
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo()));
        if (null != userPaidAlbum) {
            return;
        }

        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        userPaidAlbumMapper.insert(userPaidAlbum);
    }
}
