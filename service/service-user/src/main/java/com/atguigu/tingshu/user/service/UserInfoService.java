package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

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
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 修改用户信息
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);


}
