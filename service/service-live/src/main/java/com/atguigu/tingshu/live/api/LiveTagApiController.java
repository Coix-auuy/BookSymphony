package com.bigwharf.tingshu.live.api;

import com.bigwharf.tingshu.live.service.LiveTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/live/liveTag")
@SuppressWarnings({"unchecked", "rawtypes"})
public class LiveTagApiController {

	@Autowired
	private LiveTagService liveTagService;

}

