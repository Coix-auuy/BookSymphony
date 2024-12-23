package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
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

    @TsLogin
    @Operation(summary = "判断用户是否购买过专辑")
    @GetMapping("/isPaidAlbum/{albumId}")
    Result<Boolean> isPaidAlbum(@PathVariable("albumId") Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        Boolean result = userInfoService.isPaidAlbum(albumId, userId);
        return Result.ok(result);
    }

    /**
     * 根据专辑 Id 获取用户已购买过的声音 Id 列表
     * @param albumId
     * @return
     */
    @TsLogin
    @Operation(summary = "根据专辑 Id 获取用户已购买过的声音 Id 列表")
    @GetMapping("/findUserPaidTrackList/{albumId}")
    Result<List<Long>> findUserPaidTrackList(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        List<Long> result = userInfoService.findUserPaidTrackList(albumId, userId);
        return Result.ok(result);
    }

    /**
     * 更新 VIP 到期失效状态
     * @return
     */
    @Operation(summary = "更新Vip到期失效状态")
    @GetMapping("updateVipExpireStatus")
    public Result updateVipExpireStatus() {
        userInfoService.updateVipExpireStatus();
        return Result.ok();
    }
    /**
     * 处理用户购买记录
     * @param userPaidRecordVo
     * @return
     */
    @Operation(summary = "处理用户购买记录")
    @PostMapping("/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo) {
        // 调用服务层方法记录用户购买记录
        userInfoService.userPayRecord(userPaidRecordVo);
        return Result.ok();
    }
}

