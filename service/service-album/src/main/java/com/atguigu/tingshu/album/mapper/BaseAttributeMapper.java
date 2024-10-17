package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {

    /**
     * 根据一级分类 id 查询属性数据
     * @param category1Id
     * @return
     */
    List<BaseAttribute> selectAttribute(@Param("category1Id") Long category1Id);
}
