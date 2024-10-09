package com.bigwharf.tingshu.system.service;

import com.bigwharf.tingshu.model.system.SysPost;
import com.bigwharf.tingshu.vo.system.SysPostQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SysPostService extends IService<SysPost> {

    IPage<SysPost> selectPage(Page<SysPost> pageParam, SysPostQueryVo sysPostQueryVo);

    void updateStatus(Long id, Integer status);

    List<SysPost> findAll();
}
