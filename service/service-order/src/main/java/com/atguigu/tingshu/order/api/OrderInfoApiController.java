package com.atguigu.tingshu.order.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

    @Autowired
    private OrderInfoService orderInfoService;

    @TsLogin
    @Operation(summary = "订单结算页")
    @PostMapping("/trade")
    public Result trade(@RequestBody TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();
        OrderInfoVo orderInfoVo = orderInfoService.trade(tradeVo, userId);
        return Result.ok(orderInfoVo);
    }

    @TsLogin
    @Operation(summary = "提交订单")
    @PostMapping("/submitOrder")
    public Result submitOrder(@RequestBody OrderInfoVo orderInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        String orderNo = orderInfoService.submitOrder(orderInfoVo, userId);
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", orderNo);
        return Result.ok(map);
    }

    @Operation(summary = "根据订单号获取订单信息")
    @GetMapping("/getOrderInfo/{orderNo}")
    public Result getOrderInfo(@PathVariable String orderNo) {
        OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
        return Result.ok(orderInfo);
    }

    /**
     * 根据状态{orderStatus：未支付、已支付、已取消}，查看我的订单，分页显示
     *
     * @param page
     * @param pageSize
     * @return
     */
    @TsLogin
    @Operation(summary = "查看我的订单，分页显示")
    @GetMapping("/findUserPage/{page}/{pageSize}")
    public Result findUserPage(@PathVariable Long page, @PathVariable Long pageSize, HttpServletRequest request) {
        // 获取 ? 传递的参数
        String orderStatus = request.getParameter("orderStatus");
        Long userId = AuthContextHolder.getUserId();
        // 封装到一个 Page 对象
        Page<OrderInfo> orderInfoPage = new Page<>(page, pageSize);
        IPage<OrderInfo> orderInfoIPage = orderInfoService.findUserPage(orderInfoPage, userId, orderStatus);
        return Result.ok(orderInfoIPage);
    }
}

