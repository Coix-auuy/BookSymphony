package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {
    /**
     * 获取声音的上次跳出时间
     *
     * @param userId
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    /**
     * 更新声音播放进度
     *
     * @param userListenProcessVo
     * @param userId
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId);

    /**
     * 获取最近播放的声音
     *
     * @param userId
     * @return
     */
    Map<String, Object> getLatelyTrack(Long userId);
}
