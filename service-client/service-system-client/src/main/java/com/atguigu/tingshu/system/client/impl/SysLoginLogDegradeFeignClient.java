package com.bigwharf.tingshu.system.client.impl;


import com.bigwharf.tingshu.system.client.SysLoginLogFeignClient;
import com.bigwharf.tingshu.common.result.Result;
import com.bigwharf.tingshu.model.system.SysLoginLog;
import org.springframework.stereotype.Component;

@Component
public class SysLoginLogDegradeFeignClient implements SysLoginLogFeignClient {


    @Override
    public Result recordLoginLog(SysLoginLog sysLoginLog) {
        return null;
    }
}
