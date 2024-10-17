package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long albumId) {
        albumInfoMapper.deleteById(albumId);
        albumStatMapper.delete(new LambdaUpdateWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        albumAttributeValueMapper.delete(new LambdaUpdateWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
    }

    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        // 先获取到专辑信息表数据
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        // 后去属性值表数据
        if (null != albumInfo) {
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueMapper.selectList(new LambdaUpdateWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId)));
        }
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

    private void saveAlbumStat(Long id, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(id);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatMapper.insert(albumStat);
    }
}
