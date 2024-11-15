package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private BaseCategoryService baseCategoryService;

    /**
     * 保存专辑
     *
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "保存专辑")
    @PostMapping("/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
        // 获取用户 id, 登录成功会将 id 存储到 AuthContextHolder 中
        Long userId = AuthContextHolder.getUserId();
        if (null == userId) {
            userId = 1l;
        }
        albumInfoService.saveAlbumInfo(albumInfoVo, userId);
        return Result.ok();
    }

    /**
     * 分页查询用户专辑列表
     *
     * @param page
     * @param limit
     * @param albumInfoQuery
     * @return
     */
    @TsLogin
    @PostMapping("/findUserAlbumPage/{page}/{limit}")
    public Result findUserAlbumPage(@PathVariable Long page, @PathVariable Long limit, @RequestBody AlbumInfoQuery albumInfoQuery) {
        // 隐藏条件: 用户 id
        // Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();

        Long userId = 1l; // 测试暂时调整
        albumInfoQuery.setUserId(userId);
        // 利用 mybatis-plus 分页查询
        Page<AlbumListVo> albumListVoPage = new Page<>(page, limit);
        IPage<AlbumListVo> albumListVoIPage = albumInfoService.getUserAlbumPage(albumListVoPage, albumInfoQuery);
        return Result.ok(albumListVoIPage);
    }

    /**
     * 根据 albumId 删除专辑
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据 albumId 删除专辑")
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable Long albumId) {
        albumInfoService.removeAlbumInfo(albumId);
        return Result.ok();
    }

    /**
     * 根据 albumId 回显专辑数据
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "根据 albumId 回显专辑数据")
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable Long albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(albumId);
        return Result.ok(albumInfo);
    }

    /**
     * 更新专辑信息
     *
     * @param albumId
     * @param albumInfoVo
     * @return
     */
    @Operation(summary = "更新专辑信息")
    @PutMapping("/updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(@PathVariable Long albumId, @RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.updateAlbumInfo(albumId, albumInfoVo);
        return Result.ok();
    }

    /**
     * 获取专辑列表
     *
     * @return
     */

    @Operation(summary = "获取专辑列表")
    @GetMapping("/findUserAllAlbumList")
    public Result findUserAllAlbumList() {
        Long userId = AuthContextHolder.getUserId() == null ? 1l : AuthContextHolder.getUserId();
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(albumInfoList);
    }

    /**
     * 根据专辑 id 获取属性信息
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "获取专辑属性列表")
    @GetMapping("/findAlbumAttributeValue/{albumId}")
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(@PathVariable("albumId") Long albumId) {
        List<AlbumAttributeValue> albumAttributeValueList = albumInfoService.findAlbumAttributeValue(albumId);
        return Result.ok(albumAttributeValueList);
    }

    @GetMapping("/getAlbumStatVo/{albumId}")
    Result<AlbumStatVo> getAlbumStatVo(@PathVariable("albumId") Long albumId) {
        AlbumStatVo albumStatVo = albumInfoService.getAlbumStatVo(albumId);
        return Result.ok(albumStatVo);
    }

    ;

}

