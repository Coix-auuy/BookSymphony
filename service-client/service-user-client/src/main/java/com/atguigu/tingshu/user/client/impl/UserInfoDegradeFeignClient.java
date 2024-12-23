package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long albumId, List<Long> tarckIdList) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<VipServiceConfig> getVipServiceConfig(Long vipServiceConfigId) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<List<Long>> findUserPaidTrackList(Long albumId) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result updateVipExpireStatus() {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        System.out.println("调用失败！");
        return null;
    }
}
