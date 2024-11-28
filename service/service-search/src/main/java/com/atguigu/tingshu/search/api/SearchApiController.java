package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ItemService itemService;

    @Operation(summary = "专辑上架")
    @GetMapping("/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "专辑下架")
    @GetMapping("/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    /**
     * 批量上架:为了后续测试使用
     *
     * @return
     */
    @Operation(summary = "批量上架")
    @GetMapping("/batchUpperAlbum")
    public Result batchUpperAlbum() {
        //  循环
        for (long i = 1; i <= 1500; i++) {
            searchService.upperAlbum(i);
        }
        //  返回数据
        return Result.ok();
    }

    @Operation(summary = "检索")
    @PostMapping
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }

    @Operation(summary = "获取频道页数据")
    @GetMapping("channel/{category1Id}")
    public Result channel(@PathVariable Long category1Id) {
        List<Map<String, Object>> result = searchService.channel(category1Id);
        return Result.ok(result);
    }

    @Operation(summary = "关键字自动补全")
    @GetMapping("/completeSuggest/{keyword}")
    public Result completeSuggest(@PathVariable String keyword) {
        //  根据关键词查询补全
        List<String> list = searchService.completeSuggest(keyword);
        //  返回数据
        return Result.ok(list);
    }

    @Operation(summary = "根据专辑 Id 回显专辑数据")
    @GetMapping("/{albumId}")
    public Result getItem(@PathVariable Long albumId) {
        Map<String, Object> result = itemService.getItem(albumId);
        return Result.ok(result);
    }

    @Operation(summary = "手动调用排行榜")
    @GetMapping("/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }

    @Operation(summary = "查看排行榜")
    @GetMapping("/findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndex>> findRankingList(@PathVariable Long category1Id, @PathVariable String dimension) {
        List<AlbumInfoIndex> result = searchService.findRankingList(category1Id, dimension);
        return Result.ok(result);
    }


}

