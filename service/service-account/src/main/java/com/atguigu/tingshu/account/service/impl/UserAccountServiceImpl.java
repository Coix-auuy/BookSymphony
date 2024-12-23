package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 初始化用户账户信息
     *
     * @param userId
     */
    @Override
    public void initUserAccount(String userId) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(Long.parseLong(userId));
        userAccount.setTotalAmount(new BigDecimal("1000"));
        userAccount.setAvailableAmount(new BigDecimal("1000"));
        userAccount.setTotalIncomeAmount(new BigDecimal("1000"));
        userAccountMapper.insert(userAccount);
    }

    @Transactional
    @Override
    public AccountLockResultVo checkAndLock(AccountLockVo accountLockVo) {
        // 声明一个锁的 key
        String lockKey = "checkAndLock:" + accountLockVo.getOrderNo();
        // 声明一个数据 key
        String dataKey = "account:lock:" + accountLockVo.getOrderNo();
        // 分布式锁，防止重复订单提交
        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, "lock", 10, TimeUnit.SECONDS);
        if (!result) {
            // 当前可能查询过了，数据已在缓存，此时需要从缓存中取
            AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
            if (null != accountLockResultVo) {
                return accountLockResultVo;
            }
        }
        // 第一次执行查询
        int check = userAccountMapper.check(accountLockVo.getUserId(), accountLockVo.getAmount());
        if (check == 0) {
            // 删除锁
            redisTemplate.delete(lockKey);
            // 抛出异常
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LESS);
        }
        // 如果有足够的余额
        int lock = userAccountMapper.lock(accountLockVo.getUserId(), accountLockVo.getAmount());
        // 判断是否锁定成功
        if (lock == 0) {
            // 删除锁
            redisTemplate.delete(lockKey);
            // 抛出异常
            throw new GuiguException(ResultCodeEnum.ACCOUNT_LOCK_ERROR);
        }
        AccountLockResultVo accountLockResultVo = new AccountLockResultVo();
        accountLockResultVo.setUserId(accountLockVo.getUserId());
        accountLockResultVo.setAmount(accountLockVo.getAmount());
        accountLockResultVo.setContent(accountLockVo.getContent());
        // 写入缓存
        redisTemplate.opsForValue().set(dataKey, accountLockResultVo);
        // 记录账户资金流动明细
        addUserAccountDetail(accountLockVo.getUserId(), "锁定：" + accountLockVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_LOCK, accountLockVo.getAmount(), "lock:" + accountLockVo.getOrderNo());
        return accountLockResultVo;
    }

    @Transactional
    @Override
    public void minus(String orderNo) {
        // 声明防止重复 key
        String key = "minus:" + orderNo;
        // 声明数据 key
        String dataKey = "account:lock:" + orderNo;
        ;
        // 防止重复扣减
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(key, "lock", 10, TimeUnit.SECONDS);
        if (!lock) {
            return;
        }
        // 第一次扣减
        AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
        if (null == accountLockResultVo) {
            return;
        }
        // 执行扣减操作
        // update user_account set total_amount = total_amount - ?, lock_amount = lock_amount - ?, total_pay_amount = total_pay_amount + ? where user_id = ?
        int minusResult = userAccountMapper.minus(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
        if (minusResult == 0) {
            // 删除防止重复 key
            redisTemplate.delete(key);
            throw new GuiguException(ResultCodeEnum.ACCOUNT_MINUSLOCK_ERROR);
        }
        // 记录账户明细
        addUserAccountDetail(accountLockResultVo.getUserId(), accountLockResultVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_MINUS, accountLockResultVo.getAmount(), orderNo);
        // 删除缓存中的扣减信息
        redisTemplate.delete(dataKey);
    }

    @Override
    public void unlock(String orderNo) {
        // 防重 key
        String key = "unlock:" + orderNo;
        // 数据 key
        String dataKey = "account:lock:" + orderNo;
        // 利用 setnx 防止重复消费
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(key, "lock", 10, TimeUnit.SECONDS);
        if (!lock) {
            // 重复消费，直接停止
            return;
        }
        // 从缓存中获取数据
        AccountLockResultVo accountLockResultVo = (AccountLockResultVo) redisTemplate.opsForValue().get(dataKey);
        if (null == accountLockResultVo) {
            // 删除重复 key
            redisTemplate.delete(key);
            return;
        }
        // 解锁金额
        int unlock = userAccountMapper.unlock(accountLockResultVo.getUserId(), accountLockResultVo.getAmount());
        if (unlock == 0) {
            redisTemplate.delete(key);
            return;
        }
        // 记录日志，删除缓存数据
        addUserAccountDetail(accountLockResultVo.getUserId(), accountLockResultVo.getContent(), SystemConstant.ACCOUNT_TRADE_TYPE_UNLOCK, accountLockResultVo.getAmount(), orderNo);
        redisTemplate.delete(dataKey);
    }

    /**
     * 记录账户资金流动明细
     *
     * @param userId
     * @param title
     * @param tradeType
     * @param amount
     * @param orderNo
     */
    @Override
    public void addUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail(userId, title, tradeType, amount, orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    @Override
    public IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> userAccountDetailPage, Long userId) {
        LambdaQueryWrapper<UserAccountDetail> userAccountDetailQueryWrapper = new LambdaQueryWrapper<>();
        userAccountDetailQueryWrapper.eq(UserAccountDetail::getUserId, userId).eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        Page<UserAccountDetail> page = userAccountDetailMapper.selectPage(userAccountDetailPage, userAccountDetailQueryWrapper);
        return page;
    }

    @Override
    public IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> userAccountDetailPage, Long userId) {
        LambdaQueryWrapper<UserAccountDetail> userAccountDetailQueryWrapper = new LambdaQueryWrapper<>();
        userAccountDetailQueryWrapper.eq(UserAccountDetail::getUserId, userId).eq(UserAccountDetail::getTradeType, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
        Page<UserAccountDetail> page = userAccountDetailMapper.selectPage(userAccountDetailPage, userAccountDetailQueryWrapper);
        return page;
    }

    /**
     * 检查与扣减金额
     *
     * @param accountDeductVo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void checkAndDeduct(AccountDeductVo accountDeductVo) {
        try {
            // 1.检查及扣减余额
            int count = userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(), accountDeductVo.getAmount());
            // int i = 1/0;
            if (count == 0) {
                throw new GuiguException(400, "账户余额不足！");
            }
            // 2.新增账户变动日志
            addUserAccountDetail(
                    accountDeductVo.getUserId(),
                    accountDeductVo.getContent(),
                    SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
                    accountDeductVo.getAmount(),
                    accountDeductVo.getOrderNo()
            );
        } catch (GuiguException e) {
            throw new RuntimeException(e);
        }
    }
}
