package com.atguigu.tingshu.account.client.impl;


import com.atguigu.tingshu.account.client.UserAccountFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {

    @Override
    public Result checkAndDeduct(AccountDeductVo accountDeductVo) {
        System.out.println("调用失败！");
        return null;
    }

    @Override
    public Result<AccountLockResultVo> checkAndLock(AccountLockVo accountLockVo) {
        System.out.println("调用失败！");
        return null;
    }
}
