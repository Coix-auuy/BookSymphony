package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderDerateMapper;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import com.atguigu.tingshu.model.order.OrderDerate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserAccountFeignClient userAccountFeignClient;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private OrderDerateMapper orderDerateMapper;
    @Autowired
    private RedissonClient redissonClient;

    /**
     * @param tradeVo {itemType, itemId}
     * @param userId
     * @return
     */
    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        // 判断购买类型
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //  订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        //  减免总金额
        BigDecimal derateAmount = new BigDecimal("0.00");
        //  订单总价
        BigDecimal orderAmount = new BigDecimal("0.00");
        //  订单明细集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //  订单减免明细列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        // 根据用户 id 获取用户信息
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Assert.notNull(userInfoVoResult, "用户信息结果集为空");
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo, "用户信息为空");
        if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_ALBUM)) {
            Result<Boolean> isPaidAlbumResult = userInfoFeignClient.isPaidAlbum(tradeVo.getItemId());
            Assert.notNull(isPaidAlbumResult, "购买专辑结果集为空");
            Boolean isPaidAlbumResultData = isPaidAlbumResult.getData();
            if (isPaidAlbumResultData) { // 购买过
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            // 获取专辑信息
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            originalAmount = albumInfo.getPrice();
            // 没有购买过，封装 OrderInfoVo
            // 判断是否是 VIP，VIP 可能有折扣
            if (userInfoVo.getIsVip() == 0) {
                // 判断当前专辑是否参与折扣
                if (new BigDecimal("-1").compareTo(albumInfo.getDiscount()) != 0) { // 有折扣
                    // 减免金额
                    derateAmount = originalAmount.subtract(originalAmount.multiply(albumInfo.getDiscount()).divide(new BigDecimal("10")));
                }
                // 订单总价
                orderAmount = originalAmount.subtract(derateAmount);
            } else {
                // 计算 VIP 用户的订单金额
                if (new BigDecimal("-1").compareTo(albumInfo.getVipDiscount()) != 0) { // 有折扣
                    // 减免金额
                    derateAmount = originalAmount.subtract(originalAmount.multiply(albumInfo.getVipDiscount()).divide(new BigDecimal("10")));

                }
                // 订单总价
                orderAmount = originalAmount.subtract(derateAmount);
            }
            // 订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVoList.add(orderDetailVo);
            // 减免明细
            if (derateAmount.compareTo(new BigDecimal("0.00")) != 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setRemarks("专辑折扣");
                orderDerateVoList.add(orderDerateVo);
            }


        } else if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_TRACK)) {
            // 购买声音
            // 判断用户购买的声音级数，若 < 0，说明用户篡改了数据
            if (tradeVo.getTrackCount() < 0) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }
            // 获取购买列表
            Result<List<TrackInfo>> trackInfoListResult = trackInfoFeignClient.findPaidTrackInfoList(tradeVo.getItemId(), tradeVo.getTrackCount());
            Assert.notNull(trackInfoListResult, "购买列表结果集为空");
            List<TrackInfo> trackInfoList = trackInfoListResult.getData();
            Assert.notNull(trackInfoList, "购买列表为空");
            // 获取专辑信息：获取声音单价
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(trackInfoList.get(0).getAlbumId());
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            // 赋值：金额、订单明细
            // 金额
            originalAmount = albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(trackInfoList.size())));
            orderAmount = originalAmount;
            // 订单明细
            orderDetailVoList = trackInfoList.stream().map(trackInfo -> {
                OrderDetailVo orderDetailVo = new OrderDetailVo();
                orderDetailVo.setItemPrice(albumInfo.getPrice());
                orderDetailVo.setItemName(trackInfo.getTrackTitle());
                orderDetailVo.setItemId(trackInfo.getId());
                orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                return orderDetailVo;
            }).collect(Collectors.toList());
        } else if (tradeVo.getItemType().equals(SystemConstant.ORDER_ITEM_TYPE_VIP)) {
            // 购买 VIP 订单
            // 赋值：金额、订单明细、减免明细
            Long vipServiceConfigId = tradeVo.getItemId();
            Result<VipServiceConfig> vipServiceConfigResult = userInfoFeignClient.getVipServiceConfig(vipServiceConfigId);
            Assert.notNull(vipServiceConfigResult, "VIP 服务配置结果集为空");
            VipServiceConfig vipServiceConfig = vipServiceConfigResult.getData();
            Asserts.notNull(vipServiceConfig, "VIP 服务配置对象为空");
            // 金额
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            derateAmount = originalAmount.subtract(orderAmount);
            // 订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemName(vipServiceConfig.getName());
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(orderAmount);
            orderDetailVoList.add(orderDetailVo);
            // 订单减免明细
            if (new BigDecimal("0.00").compareTo(derateAmount) != 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setRemarks("VIP 折扣");
                orderDerateVoList.add(orderDerateVo);
            }
        }

        // 赋值
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfoVo.setTradeNo(tradeNo);
        // 防止用户重复提交订单 tradeNo -- 解决多平台覆盖问题，以及用户多订单问题
        String key = "tradeNo:" + userId + tradeNo;
        redisTemplate.opsForValue().setIfAbsent(key, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        // 设置时间戳
        orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        // 签名: 防止用户篡改数据，在数据传递过程中保障数据的安全性!
        // OrderInfoVo --> Map --> String {1. 除去 sign 字段； 2. 将 map 进行排序； 3.将 map 中的值使用 | 进行拼接得到一个字符串；4. 再拼接一个私钥（加盐） 5. 使用 MD5 再加密}
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        orderInfoVo.setSign(SignHelper.getSign(map));
        return orderInfoVo;
    }

    @GlobalTransactional(rollbackFor = Exception.class) // seata 全局事务
    @Override
    public String submitOrder(OrderInfoVo orderInfoVo, Long userId) {
        // 验证签名
        Map map = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
        map.put("payWay", "");
        SignHelper.checkSign(map);
        // 防止重复提交
        String tradeNo = orderInfoVo.getTradeNo();
        String key = "tradeNo:" + userId + tradeNo;
        // Lua 脚本 执行时是单线程的，保证操作原子性，保证多线程安全
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = (Boolean) redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(key), tradeNo);
        if (!flag) {
            // 不能重复提交订单！
            throw new GuiguException(ResultCodeEnum.ORDER_SUBMIT_REPEAT);
        }
        // 声明一个 orderNo 订单编号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        // 判断支付方式
        if (orderInfoVo.getPayWay().equals(SystemConstant.ORDER_PAY_ACCOUNT)) {
            // 余额支付
            // 检查是否有足够的余额 需要锁定余额
            // AccountLockVo accountLockVo = new AccountLockVo();
            // accountLockVo.setUserId(userId);
            // accountLockVo.setOrderNo(orderNo);
            // accountLockVo.setAmount(orderInfoVo.getOrderAmount());
            // accountLockVo.setContent("余额支付锁定");
            // 检查与扣减对象
            AccountDeductVo accountDeductVo = new AccountDeductVo();
            accountDeductVo.setUserId(userId);
            accountDeductVo.setOrderNo(orderNo);
            accountDeductVo.setAmount(orderInfoVo.getOrderAmount());
            accountDeductVo.setContent("余额支付锁定");

            try {
                // 检查、锁定金额
                // Result<AccountLockResultVo> result = userAccountFeignClient.checkAndLock(accountLockVo);
                Result result = userAccountFeignClient.checkAndDeduct(accountDeductVo);
                if (!ResultCodeEnum.SUCCESS.getCode().equals(result.getCode())) {
                    // 失败
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
                // saveOrder 同时 保存用户购买记录
                saveOrder(orderInfoVo, userId, orderNo); // 可能有异常
                // 真正扣减金额 user_account
                // kafkaService.sendMsg(KafkaConstant.QUEUE_ACCOUNT_MINUS, orderNo); // 换成了检查-扣减 checkAndDeduct
                // 记录当前用户购买信息
                UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
                userPaidRecordVo.setOrderNo(orderNo);
                userPaidRecordVo.setUserId(userId);
                userPaidRecordVo.setItemType(orderInfoVo.getItemType());
                userPaidRecordVo.setItemIdList(orderInfoVo.getOrderDetailVoList().stream().map(OrderDetailVo::getItemId).collect(Collectors.toList()));
                // 远程调用用户微服务记录购买
                Result userResult = userInfoFeignClient.savePaidRecord(userPaidRecordVo);
                if (!ResultCodeEnum.SUCCESS.getCode().equals(userResult.getCode())) {
                    throw new GuiguException(211, "新增购买记录异常");
                }
            } catch (GuiguException e) {
                // 如果出现异常，需要将余额加回去
                // kafkaService.sendMsg(KafkaConstant.QUEUE_ACCOUNT_UNLOCK, orderNo); 会导致分布式事务失效
                throw new RuntimeException(e); // 直接抛出会回滚
            }
        } else {
            // 在线支付
            saveOrder(orderInfoVo, userId, orderNo);
        }

        return orderNo;
    }

    @Override
    public void orderCancel(long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        // 当订单未支付时，取消订单
        if (null != orderInfo && SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())) {
            // 更新订单状态
            orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_CANCEL);
            orderInfo.setUpdateTime(new Date());
            orderInfoMapper.updateById(orderInfo);
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        OrderInfo orderInfo = getOrderInfoByOrderNo(orderNo);
        // 需要将支付方式由编号变为文字
        orderInfo.setPayWayName(getPayWayName(orderInfo.getPayWay()));
        return orderInfo;
    }

    /**
     * 根据状态{orderStatus：未支付、已支付、已取消}，查看我的订单，分页显示
     *
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    @Override
    public IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus) {
        // 查数据库：order_info, order_detail
        IPage<OrderInfo> orderInfoIPage = orderInfoMapper.selectUserPage(orderInfoPage, userId, orderStatus);
        orderInfoIPage.getRecords().stream().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(getOrderStatusName(orderInfo.getOrderStatus()));
        });
        return orderInfoIPage;
    }

    private String getOrderStatusName(String orderStatus) {
        //  声明订单状态名称
        String orderStatusName = "";
        //  判断
        if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
            orderStatusName = "未支付";
        } else if (SystemConstant.ORDER_STATUS_PAID.equals(orderStatus)) {
            orderStatusName = "已支付";
        } else {
            orderStatusName = "已取消";
        }
        //  返回
        return orderStatusName;
    }

    /**
     * 根据支付方式编号，返回对应的支付方式名称
     *
     * @param payWay
     * @return
     */
    private String getPayWayName(String payWay) {
        // 余额、微信、支付宝
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(payWay)) {
            return "余额";
        } else if (SystemConstant.ORDER_PAY_WAY_WEIXIN.equals(payWay)) {
            return "微信";
        } else {
            return "支付宝";
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOrder(OrderInfoVo orderInfoVo, Long userId, String orderNo) {
        // 保存数据订单信息 order_info
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoVo, orderInfo);
        orderInfo.setUserId(userId);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID);
        orderInfoMapper.insert(orderInfo);
        // 保存订单明细 order_detail
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (!CollectionUtils.isEmpty(orderDetailVoList)) {
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(orderDetailVo -> {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(orderDetailVo, orderDetail);
                orderDetail.setOrderId(orderInfo.getId());
                return orderDetail;
            }).collect(Collectors.toList());
            // 批量保存 IService ServiceImpl
            orderDetailService.saveBatch(orderDetailList);
        }
        // 保存减免明细 order_derate
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (!CollectionUtils.isEmpty(orderDerateVoList)) {
            orderDerateVoList.forEach(orderDerateVo -> {
                OrderDerate orderDerate = new OrderDerate();
                BeanUtils.copyProperties(orderDerateVo, orderDerate);
                orderDerate.setOrderId(orderInfo.getId());
                // 保存
                orderDerateMapper.insert(orderDerate);
            });
        }
        // 判断支付类型，记录当前用户购买记录信息
        if (SystemConstant.ORDER_PAY_ACCOUNT.equals(orderInfoVo.getPayWay())) {
            // 余额支付成功，保存交易数据
            orderPaySuccess(orderNo);

        } else {
            // 在线支付
            // 发送延迟消息，判断在规定的时间内是否进行了付款，如果没有付款取消订单
            // kafka 不支持延迟消息 RabbitMQ 支持延迟消息 阻塞队列支持延迟消息
            sendDelayMessage(orderInfo.getId());
        }
    }


    /**
     * 发送延迟消息
     *
     * @param orderId
     */
    private void sendDelayMessage(Long orderId) {
        // 利用 redissonClient 发送延迟消息
        try {
            // 创建一个队列
            RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(KafkaConstant.QUEUE_ORDER_CANCEL);
            // 将队列放入延迟队列中
            RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
            delayedQueue.offer(orderId.toString(), KafkaConstant.DELAY_TIME, TimeUnit.SECONDS); // 延时消息
            // 发送的内容
            log.info("添加延时队列成功 ，延迟时间：{}，订单id：{}", KafkaConstant.DELAY_TIME, orderId);
        } catch (Exception e) {
            log.error("添加延时队列失败 ，延迟时间：{}，订单id：{}", KafkaConstant.DELAY_TIME, orderId);
            e.printStackTrace();
        }
    }

    /**
     * 保存交易数据
     *
     * @param orderNo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void orderPaySuccess(String orderNo) {
        // 更新订单状态
        OrderInfo orderInfo = getOrderInfoByOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_PAID);
        orderInfo.setUpdateTime(new Date());
        orderInfoMapper.updateById(orderInfo);
        // // 区分购买类型 以发送消息的形式让用户微服务处理
        // UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        // userPaidRecordVo.setOrderNo(orderNo);
        // userPaidRecordVo.setUserId(orderInfo.getUserId());
        // userPaidRecordVo.setItemType(orderInfo.getItemType());
        // userPaidRecordVo.setItemIdList(orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList()));
        // 发送消息 保存购买记录 分布式事务会失效
        // kafkaService.sendMsg(KafkaConstant.QUEUE_USER_PAY_RECORD, JSON.toJSONString(userPaidRecordVo));
    }

    private OrderInfo getOrderInfoByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> orderInfoQueryWrapper = new LambdaQueryWrapper<>();
        orderInfoQueryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(orderInfoQueryWrapper);
        if (null != orderInfo) {
            LambdaQueryWrapper<OrderDetail> orderDetailQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailQueryWrapper.eq(OrderDetail::getOrderId, orderInfo.getId());
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);
            orderInfo.setOrderDetailList(orderDetailList);
        }
        return orderInfo;
    }
}
