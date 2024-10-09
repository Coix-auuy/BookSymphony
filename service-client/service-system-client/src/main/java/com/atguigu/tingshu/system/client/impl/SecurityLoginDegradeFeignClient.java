package com.bigwharf.tingshu.system.client.impl;


import com.bigwharf.tingshu.system.client.SecurityLoginFeignClient;
import com.bigwharf.tingshu.common.result.Result;
import com.bigwharf.tingshu.model.system.SysUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityLoginDegradeFeignClient implements SecurityLoginFeignClient {


    @Override
    public Result<SysUser> getByUsername(String username) {
        return null;
    }

    @Override
    public Result<List<String>> findUserPermsList(Long userId) {
        return null;
    }
}
