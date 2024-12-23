package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DispatchJobHandler {
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;
    @Qualifier("com.atguigu.tingshu.user.client.UserInfoFeignClient")
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    /**
     * 更新排行榜
     */
    @XxlJob("updateLatelyAlbumRankingJob")
    @Transactional(rollbackFor = Exception.class)
    public void updateLatelyAlbumRankingJob() {
        // 开始时间
        long startTime = System.currentTimeMillis();
        XxlJobLog xxlJobLog = new XxlJobLog();
        try {
            // 远程调用方法 - 更新排行榜
            log.info("更新排行榜");
            searchFeignClient.updateLatelyAlbumRanking();
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);

        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
            e.printStackTrace();
        } finally {

            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime - startTime));
            // 记录任务执行
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }

    /**
     * 更新 VIP 失效状态
     */
    @XxlJob("updateVipExpireStatusJob")
    @Transactional(rollbackFor = Exception.class)
    public void updateVipExpireStatusJob() {
        // 开始时间
        long startTime = System.currentTimeMillis();
        XxlJobLog xxlJobLog = new XxlJobLog();
        try {
            // 远程调用方法 - 更新排行榜
            log.info("更新 VIP 失效状态");
            userInfoFeignClient.updateVipExpireStatus();
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);

        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            log.error("定时任务执行失败，任务id为：{}", XxlJobHelper.getJobId());
            e.printStackTrace();
        } finally {

            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime - startTime));
            // 记录任务执行
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}