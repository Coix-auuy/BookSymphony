package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import jakarta.validation.constraints.NotEmpty;

public interface VodService {
    /**
     * 根据流媒体 id 获取流媒体信息
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getMediaInfo(@NotEmpty(message = "媒体文件Id不能为空") String mediaFileId);

    /**
     * 根据流媒体 id 删除流媒体
     * @param mediaFileId
     */
    void removeMedia(String mediaFileId);
}
