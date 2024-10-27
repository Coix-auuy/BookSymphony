package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

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
}
