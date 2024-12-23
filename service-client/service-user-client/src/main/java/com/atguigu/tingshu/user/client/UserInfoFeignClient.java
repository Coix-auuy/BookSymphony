package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-user", fallback = UserInfoDegradeFeignClient.class)
public interface UserInfoFeignClient {
    /**
     * 根据userId 获取到用户信息
     *
     * @param userId
     * @return
     */
    @GetMapping("api/user/userInfo/getUserInfoVo/{userId}")
    Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);

    @PostMapping("api/user/userInfo/userIsPaidTrack/{albumId}")
    Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long albumId, @RequestBody List<Long> tarckIdList);

    @GetMapping("api/user/userInfo/isPaidAlbum/{albumId}")
    Result<Boolean> isPaidAlbum(@PathVariable("albumId") Long albumId);

    /**
     * 根据 vipServiceConfigId 获取 VIP 服务配置信息
     * @param vipServiceConfigId
     * @return
     */
    @GetMapping("api/user/vipServiceConfig/getVipServiceConfig/{vipServiceConfigId}")
    Result<VipServiceConfig> getVipServiceConfig(@PathVariable("vipServiceConfigId") Long vipServiceConfigId);

    /**
     * 根据专辑 Id 获取用户已购买过的声音 Id 列表
     * @param albumId
     * @return
     */
    @GetMapping("api/user/userInfo/findUserPaidTrackList/{albumId}")
    Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId);

    /**
     * 更新 VIP 到期失效状态
     * @return
     */
    @GetMapping("api/user/userInfo/updateVipExpireStatus")
    Result updateVipExpireStatus();

    /**
     * 处理用户购买记录
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("api/user/userInfo/savePaidRecord")
    Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);
}