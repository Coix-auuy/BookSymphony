package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    void initUserAccount(String userId);

    /**
     * 检查与锁定金额
     *
     * @param accountLockVo
     * @return
     */
    AccountLockResultVo checkAndLock(AccountLockVo accountLockVo);

    /**
     * 监听扣减余额
     *
     * @param orderNo
     */
    void minus(String orderNo);

    /**
     * 监听解锁金额
     *
     * @param orderNo
     */
    void unlock(String orderNo);

    void addUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);

    /**
     * 分页获取充值记录
     *
     * @param userAccountDetailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> userAccountDetailPage, Long userId);

    /**
     * 分页获取消费记录
     *
     * @param userAccountDetailPage
     * @param userId
     * @return
     */
    IPage<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> userAccountDetailPage, Long userId);

    /**
     * 检查与扣减金额
     *
     * @param accountDeductVo
     */
    void checkAndDeduct(AccountDeductVo accountDeductVo);
}
