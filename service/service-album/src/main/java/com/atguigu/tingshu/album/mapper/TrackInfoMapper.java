package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 根据条件分页查询用户声音列表
     *
     * @param trackListVoPage
     * @param trackInfoQuery
     * @return
     */
    IPage<TrackListVo> selectUserTrackPage(Page<TrackListVo> trackListVoPage, @Param("trackInfoQuery") TrackInfoQuery trackInfoQuery);

    /**
     * 更新 track_info.order_num
     *
     * @param orderNum
     * @param albumId
     */
    @Update("update track_info set order_num = order_num - 1 where album_id = #{albumId} and order_num > #{orderNum} and is_deleted = 0")
    void updateOrderNum(@Param("orderNum") Integer orderNum, @Param("albumId") Long albumId);

    IPage<AlbumTrackListVo> selectAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, @Param("albumId") Long albumId);

    /**
     * 更新声音统计数据
     * @param trackId
     * @param statType
     * @param count
     */
    @Update("update track_stat set stat_num = stat_num + #{count} where track_id = #{trackId} and stat_type = #{statType}")
    void trackStatUpdate(@Param("trackId") Long trackId, @Param("statType") String statType, @Param("count") Integer count);
}
