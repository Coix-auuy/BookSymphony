package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 上传声音
     * @param file
     * @return
     */
    Map<String, Object> uploadTrack(MultipartFile file);

    /**
     * 保存声音
     * @param trackInfoVo
     * @param userId
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    /**
     * 分页查询声音列表
     * @param trackListVoPage
     * @param trackInfoQuery
     * @return
     */
    IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery);

    /**
     * 根据声音 id 删除声音
     * @param trackId
     */
    void removeTrackInfo(Long trackId);

    /**
     * 根据声音 id 查找声音信息
     * @param trackId
     * @return
     */
    TrackInfo getTrackInfoById(Long trackId);

    /**
     * 更新声音信息
     * @param trackId
     * @param trackInfoVo
     */
    void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo);

    IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId);

    /**
     * 根据声音 id，查询声音的购买分类列表
     * @param trackId
     * @param userId
     * @return
     */
    List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId);

    /**
     * "根据声音 Id 和集数获取下单付费声音列表
     * @param trackId
     * @param trackCount
     * @param userId
     * @return
     */
    List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount, Long userId);
}
