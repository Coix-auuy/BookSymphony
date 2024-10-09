package com.bigwharf.tingshu.user.service.impl;

import com.bigwharf.tingshu.model.user.UserInfo;
import com.bigwharf.tingshu.user.mapper.UserInfoMapper;
import com.bigwharf.tingshu.user.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

	@Autowired
	private UserInfoMapper userInfoMapper;

}
