package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;
    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private UserAccountService userAccountService;

    @Override
    public RechargeInfo getRechargeInfoByOrderNo(String orderNo) {
        // 根据订单号查询对象
        return getOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
    }

    /**
     * 提交充值
     *
     * @param rechargeInfoVo
     * @param userId
     * @return
     */
    @Override
    public String submitRecharge(RechargeInfoVo rechargeInfoVo, Long userId) {
        // 保存充值信息 recharge_info
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        rechargeInfo.setUserId(userId);
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        rechargeInfo.setOrderNo(orderNo);
        rechargeInfoMapper.insert(rechargeInfo);
        return orderNo;
    }

    /**
     * 支付成功：修改充值记录状态，增加金额，记录账户明细
     *
     * @param orderNo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rechargePaySuccess(String orderNo) {
        // 修改充值记录状态
        RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(new LambdaQueryWrapper<RechargeInfo>().eq(RechargeInfo::getOrderNo, orderNo));
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_PAID);
        rechargeInfoMapper.updateById(rechargeInfo);
        // 增加金额
        userAccountMapper.updateAmountByUserId(rechargeInfo.getUserId(), rechargeInfo.getRechargeAmount());
        // 记录账户明细
        userAccountService.addUserAccountDetail(rechargeInfo.getUserId(), "充值", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, rechargeInfo.getRechargeAmount(), orderNo);
    }
}
