package com.bigwharf.tingshu.account.service.impl;

import com.bigwharf.tingshu.account.mapper.UserAccountMapper;
import com.bigwharf.tingshu.account.service.UserAccountService;
import com.bigwharf.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

	@Autowired
	private UserAccountMapper userAccountMapper;

}
