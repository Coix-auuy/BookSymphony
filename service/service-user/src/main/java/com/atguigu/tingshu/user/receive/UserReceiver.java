package com.atguigu.tingshu.user.receive;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import io.micrometer.common.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Author HeZx
 * Time 2024/12/15 9:49
 * Version 1.0
 * Description:
 */
@Component
public class UserReceiver {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 记录用户购买信息
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_USER_PAY_RECORD)
    public void userPayRecord(ConsumerRecord<String, String> record) {
        String value = record.value();
        if (!StringUtils.isEmpty(value)) {
            UserPaidRecordVo userPaidRecordVo = JSON.parseObject(value, UserPaidRecordVo.class);
            userInfoService.userPayRecord(userPaidRecordVo);
        }
    }
}
