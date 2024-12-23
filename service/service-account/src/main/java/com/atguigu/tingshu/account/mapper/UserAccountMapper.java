package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {
    /**
     * 检查用户余额是否足够
     *
     * @param userId
     * @param amount
     * @return
     */
    @Select("select count(*) from user_account where user_id = #{userId} and available_amount >= #{amount} for update")
    int check(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 锁定金额
     *
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set available_amount = available_amount - #{amount}, lock_amount = lock_amount + #{amount} where user_id = #{userId}")
    int lock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣减金额
     *
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set total_amount = total_amount - #{amount}, lock_amount = lock_amount - #{amount}, total_pay_amount = total_pay_amount + #{amount} where user_id = #{userId}")
    int minus(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 解锁金额
     *
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set available_amount = available_amount + #{amount}, lock_amount = lock_amount - #{amount} where user_id = #{userId} and is_deleted = 0")
    int unlock(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("update user_account set available_amount = available_amount + #{amount}, total_amount = total_amount + #{amount}, total_income_amout = total_income_amout + #{amount} where user_id = #{userId} and is_deleted = 0")
    void updateAmountByUserId(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 检查与扣减金额
     *
     * @param userId
     * @param amount
     * @return
     */
    @Update("update user_account set total_amount = total_amount - #{amount}, available_amount = available_amount - #{amount}, total_pay_amount = total_pay_amount + #{amount} where user_id = #{userId} and available_amount >= #{amount} and is_deleted = 0")
    int checkAndDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
