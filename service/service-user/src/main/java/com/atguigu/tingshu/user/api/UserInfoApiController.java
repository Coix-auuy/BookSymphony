package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 根据 userId 获取到用户信息
     *
     * @param userId
     * @return
     */
    @Operation(summary = "根据 userId 获取到用户信息")
    @GetMapping("/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        return Result.ok(userInfoService.getUserInfo(userId));
    }

    @TsLogin(required = true)
    @Operation(summary = "判断用户是否购买声音列表")
    @PostMapping("/userIsPaidTrack/{albumId}")
    Result<Map<Long, Integer>> userIsPaidTrack(@PathVariable Long albumId, @RequestBody List<Long> tarckIdList) {
        // 获取用户 id
        Long userId = AuthContextHolder.getUserId();
        Map<Long, Integer> result = userInfoService.userIsPaidTrack(albumId, tarckIdList, userId);
        return Result.ok(result);
    }
}

