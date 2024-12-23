package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoApiController {

    @Autowired
    private RechargeInfoService rechargeInfoService;

    /**
     * 根据订单号获取充值信息
     *
     * @param orderNo
     * @return
     */
    @TsLogin
    @Operation(summary = "根据订单号获取充值信息")
    @GetMapping("/getRechargeInfo/{orderNo}")
    public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo) {
        // 调用服务层方法
        RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfoByOrderNo(orderNo);
        // 返回对象
        return Result.ok(rechargeInfo);
    }

    /**
     * 微信充值
     *
     * @param rechargeInfoVo
     * @return
     */
    @TsLogin
    @Operation(summary = "微信充值")
    @PostMapping("/submitRecharge")
    public Result submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        String orderNo = rechargeInfoService.submitRecharge(rechargeInfoVo, userId);
        Map<String, String> map = new HashMap<>();
        map.put("orderNo", orderNo);
        return Result.ok(map);
    }

}

