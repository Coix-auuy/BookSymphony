package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 微信小程序登录
     *
     * @param code
     * @return
     */
    @Operation(summary = "微信小程序登录")
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable String code) {
        // 返回数据 map 与 class 是可以替换的 map.key == class.field
        Map<String, Object> map = userInfoService.wxLogin(code);
        return Result.ok(map);
    }

    /**
     * 获取用户信息
     *
     * @return
     */
    @TsLogin
    @Operation(summary = "获取用户信息")
    @GetMapping("/getUserInfo")
    public Result getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }

    /**
     * 修改用户信息
     * @param userInfoVo
     * @return
     */
    @TsLogin
    @Operation(summary = "更新用户信息")
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        userInfoVo.setId(userId);
        userInfoService.updateUser(userInfoVo);
        return Result.ok();
    }

}
