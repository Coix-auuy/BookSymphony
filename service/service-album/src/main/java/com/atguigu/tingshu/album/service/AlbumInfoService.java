package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {

    /**
     * 根据专辑 id 获取属性列表
     * @param albumId
     * @return
     */
    List<AlbumAttributeValue> findAlbumAttributeValue(Long albumId);
    /**
     * 保存专辑
     *
     * @param albumInfoVo
     * @param userId
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    /**
     * 查询专辑分页列表
     * @param albumListVoPage
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> getUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery);

    /**
     * 根据 albumId 删除专辑
     * @param albumId
     */
    void removeAlbumInfo(Long albumId);

    AlbumInfo getAlbumInfoById(Long albumId);

    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    /**
     * 查询用户的所有专辑
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);

    AlbumStatVo getAlbumStatVo(Long albumId);
}
