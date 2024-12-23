package com.atguigu.tingshu.account.client;

import com.atguigu.tingshu.account.client.impl.UserAccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = UserAccountDegradeFeignClient.class)
public interface UserAccountFeignClient {

    /**
     * 检查与锁定账户金额
     *
     * @param accountLockVo
     * @return
     */
    @PostMapping("api/account/userAccount/checkAndLock")
    Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo);

    /**
     * 检查及扣减账户余额
     *
     * @param accountDeductVo
     * @return
     */
    @PostMapping("api/account/userAccount/checkAndDeduct")
    Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo);
}