package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private PaymentInfoService paymentInfoService;

    @TsLogin
    @Operation(summary = "微信支付")
    @PostMapping("/createJsapi/{paymentType}/{orderNo}")
    public Result createJsapi(@PathVariable String paymentType, @PathVariable String orderNo) {
        Long userId = AuthContextHolder.getUserId();
        Map<String, Object> map = wxPayService.createJsapi(paymentType, orderNo, userId);
        log.info("微信支付，订单号：{}", orderNo);
        return Result.ok(map);
    }

    /**
     * 查询交易状态
     *
     * @param orderNo
     * @return
     */
    @Operation(summary = "查询交易状态")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable String orderNo) {
        Transaction result = wxPayService.queryPayStatus(orderNo);
        log.info("查询订单状态，订单号：{}", orderNo);
        // 判断交易状态
        if (result != null && Transaction.TradeStateEnum.SUCCESS.equals(result.getTradeState())) {
            // 修改交易记录状态
            paymentInfoService.updatePaymentStatus(result);
            return Result.ok(result);
        }
        return Result.fail();
    }

    /**
     * 微信回调：微信回调用这个请求，发送交易通知（项目在本地，没有部署到公网 --> 内网穿透）
     *
     * @return
     */
    @Operation(summary = "微信异步通知")
    @PostMapping("/notify")
    public Result wxNotify(HttpServletRequest request) {
        log.info("微信支付异步回调");
        Transaction transaction = wxPayService.wxNotify(request);
        if (null != transaction && Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())) {
            // 调用修改交易状态与订单状态的方法
            paymentInfoService.updatePaymentStatus(transaction);
        }
        return Result.ok();

    }
}

