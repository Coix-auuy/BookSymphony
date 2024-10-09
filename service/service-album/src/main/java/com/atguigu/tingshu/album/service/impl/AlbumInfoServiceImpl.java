package com.bigwharf.tingshu.album.service.impl;

import com.bigwharf.tingshu.album.mapper.AlbumInfoMapper;
import com.bigwharf.tingshu.album.service.AlbumInfoService;
import com.bigwharf.tingshu.model.album.AlbumInfo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;
}
