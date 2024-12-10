package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.config.redisson.RedissonConfig;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.ame.v20190916.models.Album;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<AlbumAttributeValue> findAlbumAttributeValue(Long albumId) {
        LambdaQueryWrapper<AlbumAttributeValue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueMapper.selectList(lambdaQueryWrapper);
        return albumAttributeValueList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        try {
            // 插入AlbumInfo
            AlbumInfo albumInfo = new AlbumInfo();
            BeanUtils.copyProperties(albumInfoVo, albumInfo);
            // 设置创建该专辑的用户 id
            albumInfo.setUserId(userId);
            // 初始化状态为审核通过
            albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
            // 若为付费专辑， 设置免费声音数
            if (!SystemConstant.ALBUM_PAY_TYPE_FREE.equals(albumInfo.getPayType())) {
                albumInfo.setTracksForFree(5);
            }
            // 实体类 id 属性上有注解 @TableId(type = IdType.AUTO): 插入完成后能够自动获取到主键自增 id
            albumInfoMapper.insert(albumInfo);
            // 插入属性值
            List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
            if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
                // for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
                //     AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                //     albumAttributeValue.setAlbumId(albumInfo.getId());
                //     albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
                //     albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                //     albumAttributeValueMapper.insert(albumAttributeValue);
                // }
                val albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                    AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                    albumAttributeValue.setAlbumId(albumInfo.getId());
                    albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
                    albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                    return albumAttributeValue;
                }).collect(Collectors.toList());
                albumAttributeValueService.saveBatch(albumAttributeValueList);
            }

            // album_stat
            this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
            this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE);
            this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BROWSE);
            this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT);

            // 判断是否上架
            if ("1".equals(albumInfoVo.getIsOpen())) {
                // 上架
                kafkaService.sendMsg(KafkaConstant.QUEUE_ALBUM_UPPER, albumInfo.getId().toString());
            }


        } catch (BeansException e) {
            log.error("保存专辑失败");
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long albumId) {
        albumInfoMapper.deleteById(albumId);
        albumStatMapper.delete(new LambdaUpdateWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        albumAttributeValueMapper.delete(new LambdaUpdateWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
    }

    @Override
    @GuiGuCache(prefix = "album:") // album:albumId:lock
    public AlbumInfo getAlbumInfoById(Long albumId) {
        // AlbumInfo albumInfo = getAlbumInfoByRedisson(albumId);
        // if(null != albumInfo) {
        //     return albumInfo;
        // }
        return getAlbumInfoDB(albumId);
    }

    @NotNull
    private AlbumInfo getAlbumInfoByRedisson(Long albumId) {
        // 查询缓存
        String key = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        try {
            AlbumInfo albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(key);
            if (null == albumInfo) {
                // 缓存中没有数据，避免缓存击穿，加分布式锁
                // 声明一个锁的 key
                String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
                RLock lock = redissonClient.getLock(lockKey);
                // 如何使用 lock 进行加锁
                lock.lock();
                try {
                    // 获取到锁之后，再查询一次缓存，否则当第一个线程将数据加载到缓存后，还是会有大量线程在拿到锁之后去查数据库
                    albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(key);
                    log.info("查询了缓存……");
                    if (null != albumInfo) {
                        return albumInfo;
                    }
                    // 查询数据库
                    albumInfo = getAlbumInfoDB(albumId);
                    if (null == albumInfo) {
                        // 放入空对象，防止缓存穿透
                        redisTemplate.opsForValue().set(key, new AlbumInfo(), RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return new AlbumInfo();
                    } else {
                        // 将对象存入缓存
                        redisTemplate.opsForValue().set(key, albumInfo, RedisConstant.ALBUM_TIMEOUT, TimeUnit.SECONDS);
                        return albumInfo;
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                return albumInfo;
            }
        } catch (Exception e) {
            log.error("出现了异常： {}，专辑 Id：{} \t", e.getMessage(), albumId);
            // 发消息。。。
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private AlbumInfo getAlbumInfoDB(Long albumId) {
        // 先获取到专辑信息表数据
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        // 后去属性值表数据
        if (null != albumInfo) {
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueMapper.selectList(new LambdaUpdateWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId)));
        }
        // redissonClient.
        return albumInfo;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
        // 更新 album_info 表
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        albumInfo.setId(albumId);
        albumInfoMapper.updateById(albumInfo);
        // 更新 album_attribute_value 表
        // 删除旧的属性值信息
        albumAttributeValueMapper.delete(new LambdaUpdateWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        // 新增新的属性值信息
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)) {
            val albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                albumAttributeValue.setAlbumId(albumInfo.getId());
                albumAttributeValue.setAttributeId(albumAttributeValueVo.getAttributeId());
                albumAttributeValue.setValueId(albumAttributeValueVo.getValueId());
                return albumAttributeValue;
            }).collect(Collectors.toList());
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        // 判断是否上架
        if ("1".equals(albumInfoVo.getIsOpen())) {
            // 上架
            kafkaService.sendMsg(KafkaConstant.QUEUE_ALBUM_UPPER, albumId.toString());
        } else {
            // 下架
            kafkaService.sendMsg(KafkaConstant.QUEUE_ALBUM_LOWER, albumId.toString());
        }
    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        Page<AlbumInfo> albumInfoPage = new Page<>(1, 20);
        LambdaQueryWrapper<AlbumInfo> albumInfoQueryWrapper = new LambdaQueryWrapper<AlbumInfo>();
        albumInfoQueryWrapper.eq(AlbumInfo::getUserId, userId).select(AlbumInfo::getId, AlbumInfo::getAlbumTitle).orderByDesc(AlbumInfo::getId);
        Page<AlbumInfo> selectPage = albumInfoMapper.selectPage(albumInfoPage, albumInfoQueryWrapper);
        return selectPage.getRecords();
    }


    @Override
    public IPage<AlbumListVo> getUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        return albumInfoMapper.selectUserAlbumPage(albumListVoPage, albumInfoQuery);
    }

    @GuiGuCache(prefix = "stat:")
    @Override
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        AlbumStatVo albumStatVo = albumStatMapper.getAlbumStatVo(albumId);
        return albumStatVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trackStatUpdate(TrackStatMqVo trackStatMqVo) throws Exception {
        // 更新声音
        trackInfoMapper.trackStatUpdate(trackStatMqVo.getTrackId(), trackStatMqVo.getStatType(), trackStatMqVo.getCount());
        // 更新专辑
        if (SystemConstant.TRACK_STAT_PLAY.equals(trackStatMqVo.getStatType())) {
            albumInfoMapper.albumStatUpdate(trackStatMqVo.getAlbumId(), SystemConstant.ALBUM_STAT_PLAY, trackStatMqVo.getCount());
        }

    }

    private void saveAlbumStat(Long id, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(id);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatMapper.insert(albumStat);
    }
}
