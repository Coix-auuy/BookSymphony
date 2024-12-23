package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.midi.Track;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

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
    public IPage<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId) {
        // 根据专辑 id，获取专辑信息
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        // 根据专辑 id，获取声音列表
        IPage<AlbumTrackListVo> ipage = trackInfoMapper.selectAlbumTrackPage(albumTrackListVoPage, albumId);
        if (null == userId) {
            // 当前未登录
            // 专辑类型为 0101-免费、0102-vip 免费、0103-付费
            if (!albumInfo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
                // 需要设置不免费标识
                Integer tracksForFree = albumInfo.getTracksForFree();
                ipage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > tracksForFree).forEach(albumTrackListVo -> albumTrackListVo.setIsShowPaidMark(true));
            }
        } else {
            // 声明一个变量标识是否需要付费
            boolean isPaid = false;
            // 用户登录了
            // 获取用户信息
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
            Assert.notNull(userInfoVoResult, "用户信息结果集失败");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息为空");
            if (albumInfo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_VIPFREE)) {
                // 是 vip 但是过期了 || 不是 vip --> 付费
                if (userInfoVo.getIsVip() == 1 && userInfoVo.getVipExpireTime().before(new Date()) || userInfoVo.getIsVip() == 0) {
                    isPaid = true;
                }

            } else if (albumInfo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_REQUIRE)) {
                isPaid = true;
            }
            // 统一处理付费
            if (isPaid) {
                // 这个用户是否购买过专辑，或者购买过声音
                // 获取需要付费的声音 id 列表
                List<AlbumTrackListVo> albumTrackNeedPaidListVoList = ipage.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()).toList();
                List<Long> tarckIdList = albumTrackNeedPaidListVoList.stream().map(AlbumTrackListVo::getTrackId).toList();
                // 远程调用
                // Map: key = trackId value = 1--购买过 0--没买过
                Result<Map<Long, Integer>> resultMap = userInfoFeignClient.userIsPaidTrack(albumId, tarckIdList);
                Assert.notNull(resultMap, "返回结果为空");
                Map<Long, Integer> map = resultMap.getData();
                Assert.notNull(map, "返回结果为空");
                for (AlbumTrackListVo albumTrackListVo : albumTrackNeedPaidListVoList) {
                    albumTrackListVo.setIsShowPaidMark(map.get(albumTrackListVo.getTrackId()) != 1);
                }
            }
        }
        return ipage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId, Long userId) {
        // 根据声音 Id 获取当前声音对象
        TrackInfo trackInfo = this.getById(trackId);
        // 根据专辑 Id 获取专辑信息：后续获取单条声音价格
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());
        // 根据专辑 Id 获取到用户已购买过的声音 Id 集合
        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        Assert.notNull(trackIdListResult, "声音 Id 集合结果集为空");
        List<Long> trackIdList = trackIdListResult.getData();
        Assert.notNull(trackIdList, "声音 Id 集合为空");

        // 根据专辑 Id 和 trackInfo 中的 orderNum 获取当前声音之后所有的声音信息
        LambdaQueryWrapper<TrackInfo> trackInfoQueryWrapper = new LambdaQueryWrapper<>();
        trackInfoQueryWrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId()).gt(TrackInfo::getOrderNum, trackInfo.getOrderNum()).orderByAsc(TrackInfo::getOrderNum);
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(trackInfoQueryWrapper);
        // 排除购买过的
        List<TrackInfo> buyList = trackInfoList.stream().filter(track -> !trackIdList.contains(track.getId())).collect(Collectors.toList());
        // 构造声音分集购买数据列表
        List<Map<String, Object>> list = new ArrayList<>();
        // 本集
        {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "本集");
            map.put("price", albumInfo.getPrice());
            map.put("trackCount", 0);
            list.add(map);
        }
        // 后 n 集
        int buyListSize = buyList.size();
        if (buyListSize > 0) {
            int[] thresholds = {10, 20, 30, 40, 50};
            for (int threshold : thresholds) {
                Map<String, Object> map = new HashMap<>();
                BigDecimal price;
                if (buyListSize > threshold) {
                    // BigDecimal 数据类型，传递参数时，使用 String 数据类型，防止精度丢失
                    price = albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(threshold)));
                    map.put("name", "后" + threshold + "集");
                    map.put("price", price);
                    map.put("trackCount", threshold);
                    list.add(map);
                } else {
                    price = albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(buyListSize)));
                    map.put("name", "后" + buyListSize + "集");
                    map.put("price", price);
                    map.put("trackCount", buyListSize);
                    list.add(map);
                    break;
                }
            }
        }
        return list;
    }

    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount, Long userId) {
        // 根据当前声音 Id 获取到声音对象
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        // 根据专辑 Id 获取到用户已购买过的声音 Id 集合
        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        Assert.notNull(trackIdListResult, "声音 Id 集合结果集为空");
        List<Long> trackIdList = trackIdListResult.getData();
        Assert.notNull(trackIdList, "声音 Id 集合为空");
        // 查询购买的声音列表
        if(trackCount > 0) {
            LambdaQueryWrapper<TrackInfo> trackInfoQueryWrapper = new LambdaQueryWrapper<>();
            trackInfoQueryWrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId()).gt(TrackInfo::getOrderNum, trackInfo.getOrderNum());
            if(!CollectionUtils.isEmpty(trackIdList)) {
                trackInfoQueryWrapper.notIn(TrackInfo::getId, trackIdList);
            }
            trackInfoQueryWrapper.orderByAsc(TrackInfo::getOrderNum).last("limit " + trackCount);
            return trackInfoMapper.selectList(trackInfoQueryWrapper);
        } else {
            return List.of(trackInfo);
        }
    }

    private void saveTrackStat(Long trackId, String statPlay) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statPlay);
        trackStat.setStatNum(0);
        trackStatMapper.insert(trackStat);
    }
}
