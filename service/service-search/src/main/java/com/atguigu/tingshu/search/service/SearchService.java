package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {


    void upperAlbum(Long albumId);

    void lowerAlbum(Long albumId);

    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 获取频道页数据
     *
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);

    /**
     * 关键字自动补全
     *
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 存储排行榜数据到缓存
     */
    void updateLatelyAlbumRanking();

    /**
     * 查看排行榜
     *
     * @param category1Id
     * @param dimension
     * @return
     */
    List<AlbumInfoIndex> findRankingList(Long category1Id, String dimension);
}
