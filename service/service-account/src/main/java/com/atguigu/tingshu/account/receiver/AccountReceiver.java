package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.mysql.cj.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Author HeZx
 * Time 2024/10/27 15:27
 * Version 1.0
 * Description:
 */
@Component
public class AccountReceiver {
    @Autowired
    private UserAccountService userAccountService;

    /**
     * 监听消息，实现用户账户初始化
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void userRegister(ConsumerRecord<String, String> record){
        // 获取发送的消息
        String userId = record.value();
        if(!StringUtils.isNullOrEmpty(userId)) {
            userAccountService.initUserAccount(userId);
        }
    }
}
