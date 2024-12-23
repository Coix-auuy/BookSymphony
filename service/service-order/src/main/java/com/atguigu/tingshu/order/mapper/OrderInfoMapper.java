package com.atguigu.tingshu.order.mapper;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 根据状态{orderStatus：未支付、已支付、已取消}，查看我的订单，分页显示
     *
     * @param orderInfoPage MyBatis-Plus 插件自动将分页条件追加到 SQL 语句末尾
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> selectUserPage(Page<OrderInfo> orderInfoPage, @Param("userId") Long userId, @Param("orderStatus") String orderStatus);
}
