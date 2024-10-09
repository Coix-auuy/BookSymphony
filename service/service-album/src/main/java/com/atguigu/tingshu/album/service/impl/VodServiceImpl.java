package com.bigwharf.tingshu.album.service.impl;

import com.bigwharf.tingshu.album.config.VodConstantProperties;
import com.bigwharf.tingshu.album.service.VodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

}
