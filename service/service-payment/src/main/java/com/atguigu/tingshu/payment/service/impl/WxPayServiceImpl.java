package com.atguigu.tingshu.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private RSAAutoCertificateConfig config;
    @Autowired
    private WxPayV3Config wxPayV3Config;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;

    /**
     * 微信支付
     *
     * @param paymentType
     * @param orderNo
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> createJsapi(String paymentType, String orderNo, Long userId) {
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            // 1. 根据订单编号，查询当前订单信息
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
            Asserts.notNull(orderInfoResult, "订单信息结果集为空");
            OrderInfo orderInfo = orderInfoResult.getData();
            Asserts.notNull(orderInfo, "订单信息为空");
            if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderInfo.getOrderStatus())) {
                // 订单已取消，不生成二维码
                return null;
            }
        }
        // 保存支付交易记录到 payment_info 表中
        // 保存支付记录
        PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, userId, orderNo);
        // 构建 service
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
        PrepayRequest request = new PrepayRequest();
        // 填充预下单参数
        Amount amount = new Amount(); // 订单总金额，单位为分。
        amount.setTotal(1);
        request.setAmount(amount);
        request.setAppid(wxPayV3Config.getAppid());
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setDescription(paymentInfo.getContent());
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
        request.setOutTradeNo(orderNo);
        Payer payer = new Payer();
        // 远程调用获取用户 openId
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Asserts.notNull(userInfoVoResult, "用户信息结果集为空");
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Asserts.notNull(userInfoVo, "用户信息为空");
        payer.setOpenid(userInfoVo.getWxOpenId());
        request.setPayer(payer);
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

        // 构造返回结果
        Map<String, Object> result = new HashMap();
        result.put("timeStamp", response.getTimeStamp()); // 时间戳
        result.put("nonceStr", response.getNonceStr()); // 随机字符串
        result.put("package", response.getPackageVal()); // 订单详情扩展字符串
        result.put("signType", response.getSignType()); // 签名方式
        result.put("paySign", response.getPaySign()); // 签名
        return result;
    }

    @Override
    public Transaction queryPayStatus(String orderNo) {
        try {
            //	构建service
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(wxPayV3Config.getMerchantId());
            queryRequest.setOutTradeNo(orderNo);
            // 获取支付结果
            Transaction result = service.queryOrderByOutTradeNo(queryRequest);
            log.info("Transaction:\t{}", JSON.toJSONString(result));
            return result;
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 微信回调函数
     * @param request
     * @return
     */
    @Override
    public Transaction wxNotify(HttpServletRequest request) {
        // 1.回调通知的验签与解密
        // 从request头信息获取参数
        // HTTP 头 Wechatpay-Signature 应答的微信支付签名
        // HTTP 头 Wechatpay-Nonce 签名中的随机数
        // HTTP 头 Wechatpay-Timestamp 签名中的时间戳
        // HTTP 头 Wechatpay-Serial 微信支付平台证书的序列号
        // HTTP 头 Wechatpay-Signature-Type
        // HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
        String wechatPaySerial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signature = request.getHeader("Wechatpay-Signature");
        // 调用工具类来获取请求体数据
        String requestBody = PayUtil.readData(request);

        // 2.构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();

        // 3.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);
        // 4.以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        log.info("成功解析：{}", JSON.toJSONString(transaction));
        return transaction;
    }
}
