package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface UserInfoService extends IService<UserInfo> {
    /**
     * 微信小程序登录
     *
     * @param code
     * @return
     */
    Map<String, Object> wxLogin(String code);

    /**
     * 获取用户信息
     *
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 修改用户信息
     *
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);


    Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> tarckIdList, Long userId);

    /**
     * 判断用户是否购买过专辑
     *
     * @param albumId
     * @param userId
     * @return
     */
    Boolean isPaidAlbum(Long albumId, Long userId);

    /**
     * 根据专辑 Id 获取用户已购买过的声音 Id 列表
     *
     * @param albumId
     * @param userId
     * @return
     */
    List<Long> findUserPaidTrackList(Long albumId, Long userId);

    /**
     * 记录用户购买信息
     * @param userPaidRecordVo
     */
    void userPayRecord(UserPaidRecordVo userPaidRecordVo);

    /**
     * 更新 VIP 到期时间失效状态
     */
    void updateVipExpireStatus();


}
