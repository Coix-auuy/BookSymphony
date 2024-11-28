package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumListVo> selectUserAlbumPage(Page<AlbumListVo> albumListVoPage, @Param("albumInfoQuery") AlbumInfoQuery albumInfoQuery);

    /**
     * 更新专辑统计数据
     * @param albumId
     * @param albumStat
     * @param count
     */
    @Update("update album_stat set stat_num = stat_num + #{count} where album_id = #{albumId} and stat_type = #{albumStat}")
    void albumStatUpdate(@Param("albumId") Long albumId, @Param("albumStat") String albumStat, @Param("count") Integer count);
}



