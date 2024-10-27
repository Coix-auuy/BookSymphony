package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.prometheus.client.Summary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    /**
     * 使用腾讯云点播上传声音
     *
     * @param file
     * @return
     */
    @Operation(summary = "上传声音")
    @PostMapping("/uploadTrack")
    public Result uploadTrack(MultipartFile file) {
        Map<String, Object> result = trackInfoService.uploadTrack(file);
        return Result.ok(result);
    }

    /**
     * 保存声音信息到数据库中
     *
     * @param trackInfoVo
     * @return
     */
    @Operation(summary = "保存声音")
    @PostMapping("/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        // 获取用户 id
        Long userId = AuthContextHolder.getUserId() != null ? AuthContextHolder.getUserId() : 1L;
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        return Result.ok();
    }

    /**
     * 查询声音列表
     *
     * @param page
     * @param limit
     * @param trackInfoQuery
     * @return
     */
    @TsLogin
    @Operation(summary = "查询声音列表")
    @PostMapping("/findUserTrackPage/{page}/{limit}")
    public Result findUserTrackPage(@PathVariable Long page, @PathVariable Long limit, @RequestBody TrackInfoQuery trackInfoQuery) {
        // 获取用户 id
        Long userId = AuthContextHolder.getUserId() != null ? AuthContextHolder.getUserId() : 1L;
        trackInfoQuery.setUserId(userId);
        // 封装 Page 对象
        Page<TrackListVo> trackListVoPage = new Page<>(page, limit);
        // 调用服务层方法
        IPage<TrackListVo> trackListVoIPage = trackInfoService.findUserTrackPage(trackListVoPage, trackInfoQuery);
        return Result.ok(trackListVoIPage);
    }

    /**
     * 根据声音 id 删除声音
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音 id 删除声音")
    @DeleteMapping("/removeTrackInfo/{trackId}")
    public Result removeTrackInfo(@PathVariable Long trackId) {
        trackInfoService.removeTrackInfo(trackId);
        return Result.ok();
    }

    /**
     * 根据声音 id 获取声音信息
     *
     * @param trackId
     * @return
     */
    @Operation(summary = "根据声音 id 获取声音信息")
    @GetMapping("/getTrackInfo/{trackId}")
    public Result getAlbumInfo(@PathVariable Long trackId) {
        // 调用服务层方法
        TrackInfo trackInfo = trackInfoService.getTrackInfoById(trackId);
        return Result.ok(trackInfo);
    }

    /**
     * 更新声音信息
     *
     * @param trackId
     * @param trackInfoVo
     * @return
     */
    @Operation(summary = "更新声音信息")
    @PutMapping("/updateTrackInfo/{trackId}")
    public Result updateTrackInfo(@PathVariable Long trackId, @RequestBody TrackInfoVo trackInfoVo) {
        // 调用服务层方法
        trackInfoService.updateTrackInfo(trackId, trackInfoVo);
        return Result.ok();
    }
}

