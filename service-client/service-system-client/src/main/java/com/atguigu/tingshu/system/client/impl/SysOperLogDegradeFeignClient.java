package com.bigwharf.tingshu.system.client.impl;


import com.bigwharf.tingshu.system.client.SysOperLogFeignClient;
import com.bigwharf.tingshu.common.result.Result;
import com.bigwharf.tingshu.model.system.SysOperLog;
import org.springframework.stereotype.Component;

@Component
public class SysOperLogDegradeFeignClient implements SysOperLogFeignClient {


    @Override
    public Result saveSysLog(SysOperLog sysOperLog) {
        return null;
    }
}
