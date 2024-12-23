package com.atguigu.tingshu.user.strategy.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.strategy.PaymentStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.j2objc.annotations.AutoreleasePool;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Author HeZx
 * Time 2024/12/21 16:43
 * Version 1.0
 * Description: 购买 Vip
 */
@Service(SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VipPaymentStrategy implements PaymentStrategy {
    @Autowired
    private UserVipServiceMapper userVipServiceMapper;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public void processPayment(UserPaidRecordVo userPaidRecordVo) {
        // VIP
        UserVipService userVipService = userVipServiceMapper.selectOne(new LambdaQueryWrapper<UserVipService>().eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo()));

        if (null != userVipService) {
            return;
        }
        // 查询用户购买的 vip 时间
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        // 保存数据
        userVipService = new UserVipService();
        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        userVipService.setUserId(userPaidRecordVo.getUserId());
        userVipService.setStartTime(new Date());
        // 过期时间
        // 第一次购买：当前时间 + 购买月份；续费：没过期 = 当前过期时间 + 续费时间，过期了 = 当前时间 + 购买月份
        // 查询购买记录
        UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
        Date expireTime;
        if (null != userInfo && userInfo.getIsVip() == 1 && userInfo.getVipExpireTime().after(new Date())) {
            // 续费
            LocalDateTime localDate = new LocalDateTime(userInfo.getVipExpireTime());
            expireTime = localDate.plusMonths(serviceMonth).toDate();
        } else {
            expireTime = new LocalDateTime().plusMonths(serviceMonth).toDate();
        }
        userVipService.setExpireTime(expireTime);
        // int i = 1/0;
        userVipServiceMapper.insert(userVipService);
        // 修改用户状态 普通用户 --> VIP
        userInfo.setIsVip(1);
        userInfo.setVipExpireTime(expireTime);
        userInfoMapper.updateById(userInfo);
    }
}
