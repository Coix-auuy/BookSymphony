package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;

    @TsLogin
    @Operation(summary = "获取账户余额")
    @GetMapping("/getAvailableAmount")
    public Result getAvailableAmount() {
        Long userId = AuthContextHolder.getUserId();
        UserAccount userAccount = userAccountService.getOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId));
        if (null == userAccount) {
            return Result.fail();
        }
        return Result.ok(userAccount.getAvailableAmount());
    }

    /**
     * 检查与锁定账户金额
     *
     * @param accountLockVo
     * @return
     */
    @Operation(summary = "检查与锁定金额")
    @PostMapping("/checkAndLock")
    Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo) {
        AccountLockResultVo accountLockResultVo = userAccountService.checkAndLock(accountLockVo);
        return Result.ok(accountLockResultVo);
    }

    /**
     * 分页查询充值记录 trade_type = 1201
     *
     * @param page
     * @param pageSize
     * @return
     */
    @TsLogin
    @Operation(summary = "分页查询充值记录")
    @GetMapping("/findUserRechargePage/{page}/{pageSize}")
    public Result findUserRechargePage(@PathVariable Long page, @PathVariable Long pageSize) {
        Long userId = AuthContextHolder.getUserId();
        Page<UserAccountDetail> userAccountDetailPage = new Page<>(page, pageSize);
        IPage<UserAccountDetail> iPage = userAccountService.findUserRechargePage(userAccountDetailPage, userId);
        return Result.ok(iPage);
    }

    /**
     * 分页查询购买记录 trade_type = 1204
     *
     * @param page
     * @param pageSize
     * @return
     */
    @TsLogin
    @Operation(summary = "分页查询充值记录")
    @GetMapping("/findUserConsumePage/{page}/{pageSize}")
    public Result findUserConsumePage(@PathVariable Long page, @PathVariable Long pageSize) {
        Long userId = AuthContextHolder.getUserId();
        Page<UserAccountDetail> userAccountDetailPage = new Page<>(page, pageSize);
        IPage<UserAccountDetail> iPage = userAccountService.findUserConsumePage(userAccountDetailPage, userId);
        return Result.ok(iPage);
    }

    /**
     * 检查及扣减账户余额
     *
     * @param accountDeductVo
     * @return
     */
    @Operation(summary = "检查及扣减账户金额")
    @PostMapping("/checkAndDeduct")
    Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo) {
        // 调用业务逻辑完成账户余额扣款
        userAccountService.checkAndDeduct(accountDeductVo);
        return Result.ok();
    }
}

