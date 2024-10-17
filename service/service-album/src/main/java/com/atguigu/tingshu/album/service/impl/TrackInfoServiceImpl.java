package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;
    @Autowired
    private VodConstantProperties vodConstantProperties;
    @Autowired
    private TrackStatMapper trackStatMapper;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;
    @Autowired
    private VodService vodService;


    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        Map<String, Object> map = new HashMap<>();
        // 获取路径
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        // 初始化一个上传客户端对象
        VodUploadClient client = new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        // 构造上传请求对象，设置媒体本地上传路径
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(tempPath);
        // 调用上传方法
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
            return map;
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("Upload Err", e);
            throw new GuiguException(ResultCodeEnum.FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        // track_info 声音主表
        TrackInfo trackInfo = new TrackInfo();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 设置用户 id
        trackInfo.setUserId(userId);
        // 设置 order_num
        LambdaQueryWrapper<TrackInfo> queryWrapper = new LambdaQueryWrapper<TrackInfo>().select(TrackInfo::getOrderNum).eq(TrackInfo::getAlbumId, trackInfoVo.getAlbumId()).orderByDesc(TrackInfo::getId).last(" limit 1");
        TrackInfo trackInfoWithOrderNum = trackInfoMapper.selectOne(queryWrapper);
        trackInfo.setOrderNum(null != trackInfoWithOrderNum ? trackInfoWithOrderNum.getOrderNum() + 1 : 1);
        // media_duration media_size media_type media_url 通过 media_file_id 从 云点播 获取
        // 调用方法 从云点播获取
        TrackMediaInfoVo trackMediaInfoVo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
        trackInfo.setMediaDuration(trackMediaInfoVo.getDuration());
        trackInfo.setMediaSize(trackMediaInfoVo.getSize());
        trackInfo.setMediaType(trackMediaInfoVo.getType());
        trackInfo.setMediaUrl(trackMediaInfoVo.getMediaUrl());
        trackInfoMapper.insert(trackInfo);

        // track_stat
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);
        // 修改 album_info 表中的 include_track_count 字段
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        albumInfoMapper.updateById(albumInfo);
    }

    @Override
    public IPage<TrackListVo> findUserTrackPage(Page<TrackListVo> trackListVoPage, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.selectUserTrackPage(trackListVoPage, trackInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long trackId) {
        // 删除track_info、track_stat，更新 album_info.include_track_count
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        trackInfoMapper.deleteById(trackId);
        trackStatMapper.delete(new LambdaUpdateWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);
        // 更新 track_info.order_num
        trackInfoMapper.updateOrderNum(trackInfo.getOrderNum(), trackInfo.getAlbumId());
        // 删除点播云中的流媒体声音
        vodService.removeMedia(trackInfo.getMediaFileId());
    }

    @Override
    public TrackInfo getTrackInfoById(Long trackId) {
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);

        return trackInfo;
    }

    @Override
    public void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo) {
        // 在数据库中先查询到未做修改的声音信息
        TrackInfo trackInfo = this.getById(trackId);
        String mediaFileId = trackInfo.getMediaFileId();
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        // 若声音媒体文件id有修改（即重新上传了新的声音）
        if (!trackInfoVo.getMediaFileId().equals(mediaFileId)) {
            // 则从云点播中获取新的流媒体信息
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            trackInfo.setMediaSize(mediaInfo.getSize());
            trackInfo.setMediaDuration(mediaInfo.getDuration());
            trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
            trackInfo.setMediaType(mediaInfo.getType());
            // 删除云点播中的声音记录
            vodService.removeMedia(mediaFileId);
        }
        trackInfoMapper.updateById(trackInfo);
    }

    private void saveTrackStat(Long trackId, String statPlay) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statPlay);
        trackStat.setStatNum(0);
        trackStatMapper.insert(trackStat);
    }
}
