package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import com.mysql.cj.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Author HeZx
 * Time 2024/11/12 10:08
 * Version 1.0
 * Description:
 */
@Component
public class SearchReceive {
    @Autowired
    private SearchService searchService;

    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void albumUpper(ConsumerRecord<String, String> consumerRecord) {
        String albumId = consumerRecord.value();
        if (!StringUtils.isNullOrEmpty(albumId)) {
            searchService.upperAlbum(Long.parseLong(albumId));
        }
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void albumLower(ConsumerRecord<String, String> consumerRecord) {
        String albumId = consumerRecord.value();
        if (!StringUtils.isNullOrEmpty(albumId)) {
            searchService.lowerAlbum(Long.parseLong(albumId));
        }
    }
}
