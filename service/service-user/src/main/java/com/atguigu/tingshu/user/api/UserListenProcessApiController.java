package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    @TsLogin
    @Operation(summary = "获取声音的上次跳出时间")
    @GetMapping("/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
        //	获取用户Id
        Long userId = AuthContextHolder.getUserId();
        //	调用服务层方法
        BigDecimal trackBreakSecond = userListenProcessService.getTrackBreakSecond(userId, trackId);
        //	返回数据
        return Result.ok(trackBreakSecond);
    }

    @TsLogin
    @Operation(summary = "更新声音播放进度")
    @PostMapping("/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
        Long userId = AuthContextHolder.getUserId();
        userListenProcessService.updateListenProcess(userListenProcessVo, userId);
        return Result.ok();
    }

    @TsLogin
    @Operation(summary = "获取最近播放的声音")
    @GetMapping("/getLatelyTrack")
    public Result getLatelyTrack() {
        Long userId = AuthContextHolder.getUserId();
        Map<String, Object> result =  userListenProcessService.getLatelyTrack(userId);
        return Result.ok(result);
    }

}

