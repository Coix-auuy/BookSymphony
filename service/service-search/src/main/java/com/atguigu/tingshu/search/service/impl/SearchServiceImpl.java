package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.base.BaseEntity;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchServiceImpl implements SearchService {
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private AlbumIndexRepository albumIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ElasticsearchClient elasticsearchClient; // nacos 里有配置 es，这里会自动注入，不需要配置 bean

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    @Override
    public void upperAlbum(Long albumId) {
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        // 赋值
        // 专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "返回专辑结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "返回专辑为空");
            BeanUtils.copyProperties(albumInfo, albumInfoIndex);
            return albumInfo;
        }, threadPoolExecutor); // 使用自定义线程池，若不指定则默认使用 ForkJoinPool
        // 分类信息
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(categoryViewResult, "返回分类结果集为空");
            BaseCategoryView categoryView = categoryViewResult.getData();
            Assert.notNull(categoryView, "返回分类为空");
            // 三级分类 id 已经赋值过
            albumInfoIndex.setCategory1Id(categoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(categoryView.getCategory2Id());
        }, threadPoolExecutor);
        // 用户信息 announcerName
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoResult, "返回用户信息结果集为空");
            UserInfoVo userInfoVo = userInfoResult.getData();
            Assert.notNull(userInfoVo, "返回用户信息为空");
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolExecutor);
        // 统计信息 (自己实现)
        CompletableFuture<Void> StatCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            Assert.notNull(albumStatVoResult, "返回专辑统计结果集为空");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            Assert.notNull(albumStatVo, "返回专辑统计信息为空");
            albumInfoIndex.setPlayStatNum(albumStatVo.getPlayStatNum());
            albumInfoIndex.setSubscribeStatNum(albumStatVo.getSubscribeStatNum());
            albumInfoIndex.setBuyStatNum(albumStatVo.getBuyStatNum());
            albumInfoIndex.setCommentStatNum(albumStatVo.getCommentStatNum());
            // 热度排名
            double hotScore = 0.1 * albumStatVo.getPlayStatNum() + 0.2 * albumStatVo.getSubscribeStatNum() + 0.3 * albumStatVo.getBuyStatNum() + 0.4 * albumStatVo.getCommentStatNum();
            albumInfoIndex.setHotScore(hotScore);
        }, threadPoolExecutor);
        // 专辑属性信息
        CompletableFuture<Void> AttrCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<List<AlbumAttributeValue>> albumAttributeValueResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
            Assert.notNull(albumAttributeValueResult, "返回专辑属性结果集为空");
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueResult.getData();
            Assert.notNull(albumAttributeValueList, "返回专辑属性为空");
            albumInfoIndex.setAttributeValueIndexList(albumAttributeValueList.stream().map(albumAttributeValue -> {
                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                BeanUtils.copyProperties(albumAttributeValue, attributeValueIndex);
                return attributeValueIndex;
            }).collect(Collectors.toList()));
        }, threadPoolExecutor);
        // 将 5 个线程进行组合
        /*
        这段代码的功能是等待所有异步任务完成后再继续执行后续操作。
        具体来说：
            CompletableFuture.allOf() 方法用于等待所有传入的 CompletableFuture 对象完成。
            .join() 方法用于阻塞当前线程，直到所有 CompletableFuture 对象都完成。
         */
        CompletableFuture.allOf(albumInfoCompletableFuture, categoryCompletableFuture, userCompletableFuture, StatCompletableFuture, AttrCompletableFuture).join();
        // 保存 albumInfoIndex 到索引库，使用 API 操作 ES
        albumIndexRepository.save(albumInfoIndex);

        // 将部分数据保存到提词器：实现关键字补全
        // 专辑标题提词器
        SuggestIndex albumTitlesuggestIndex = new SuggestIndex();
        albumTitlesuggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        albumTitlesuggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        albumTitlesuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        albumTitlesuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
        albumTitlesuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
        suggestIndexRepository.save(albumTitlesuggestIndex);

        //  专辑简介提词
        SuggestIndex albumIntroSuggestIndex = new SuggestIndex();
        albumIntroSuggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        albumIntroSuggestIndex.setTitle(albumInfoIndex.getAlbumIntro());
        albumIntroSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumIntro()}));
        albumIntroSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumIntro())}));
        albumIntroSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumIntro())}));
        suggestIndexRepository.save(albumIntroSuggestIndex);

        // 专辑主播提词
        SuggestIndex announcerSuggestIndex = new SuggestIndex();
        announcerSuggestIndex.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        announcerSuggestIndex.setTitle(albumInfoIndex.getAnnouncerName());
        announcerSuggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAnnouncerName()}));
        announcerSuggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAnnouncerName())}));
        announcerSuggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAnnouncerName())}));
        suggestIndexRepository.save(announcerSuggestIndex);
    }

    @Override
    public void lowerAlbum(Long albumId) {
        albumIndexRepository.deleteById(albumId);
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {


        /*
        AlbumSearchResponseVo：
            private List<AlbumInfoIndexVo> list = new ArrayList<>(); 根据用户输入的检索条件，生成 dsl 语句，获取结果集
            private Long total;//总记录数
            private Integer pageSize;//每页显示的内容
            private Integer pageNo;//当前页面
            private Long totalPages;
         */
        // 获取一个 SearchRequest 对象
        SearchRequest searchRequest = this.queryBuildDsl(albumIndexQuery);
        // 调用 api 操作 ES
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 定义一个方法给 albumSearchResponseVo 赋值 List<AlbumInfoIndexVo> list、Long total
        AlbumSearchResponseVo albumSearchResponseVo = this.parseResultData(searchResponse);
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        Long totalPages = (albumSearchResponseVo.getTotal() + albumIndexQuery.getPageSize() - 1) / albumIndexQuery.getPageSize();
        albumSearchResponseVo.setTotalPages(totalPages);
        return albumSearchResponseVo;
    }

    @Override
    public List<Map<String, Object>> channel(Long category1Id) {
        // 根据一级分类 id 找到三级分类对象数据
        // 远程调用，获取三级分类数据
        Result<List<BaseCategory3>> baseCategory3ListResult = categoryFeignClient.findTopBaseCategory3(category1Id);
        Assert.notNull(baseCategory3ListResult, "三级分类结果集为空");
        List<BaseCategory3> baseCategory3List = baseCategory3ListResult.getData();
        Assert.notNull(baseCategory3List, "三级分类集合为空");
        // 将三级分类数据转换为一个 map<Long, BaseCategory3>
        Map<Long, BaseCategory3> baseCategory3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseEntity::getId, baseCategory3 -> baseCategory3));
        // 获取三级分类集合中的 id 列表
        List<Long> category3IdList = baseCategory3List.stream().map(BaseCategory3::getId).toList();
        // 生成 dsl 语句 ts.value(List<FieldValue>)  List<FieldValue> 这个对象应该是三级分类 id 集合
        List<FieldValue> fieldValueList = category3IdList.stream().map(FieldValue::of).toList();
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(f -> f.index("albuminfo").query(q -> q.terms(t -> t.field("category3Id").terms(tt -> tt.value(fieldValueList)))).aggregations("groupByCategory3IdAgg", a -> a.terms(t -> t.field("category3Id").size(10)).aggregations("topSixHotScoreAgg", ag -> ag.topHits(fg -> fg.size(6).sort(s -> s.field(sf -> sf.field("hotScore").order(SortOrder.Desc)))))), AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Aggregate> aggregations = searchResponse.aggregations();
        Aggregate groupByCategory3IdAgg = aggregations.get("groupByCategory3IdAgg");
        // lterms() 返回 groupByCategory3IdAgg 下的内容
        List<Map<String, Object>> mapList = groupByCategory3IdAgg.lterms().buckets().array().stream().map(bucket -> {
            Map<String, Object> map = new HashMap<>();
            // 获取三级分类 id
            long category3Id = bucket.key();
            // 获取三级分类对象
            map.put("baseCategory3", baseCategory3Map.get(category3Id));
            // 获取三级分类 id 下的专辑数据集合
            List<AlbumInfoIndex> albumInfoIndexList = bucket.aggregations().get("topSixHotScoreAgg").topHits().hits().hits().stream().map(hit -> {
                String jsonStr = hit.source().toString();
                return JSON.parseObject(jsonStr, AlbumInfoIndex.class);
            }).toList();

            map.put("list", albumInfoIndexList);
            return map;
        }).toList();
        return mapList;
    }

    @Override
    public List<String> completeSuggest(String keyword) {
        // 查询数据
        SearchResponse<SuggestIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(f -> f.index("suggestinfo").suggest(s -> s.suggesters("suggestionKeyword", sf -> sf.prefix(keyword).completion(c -> c.field("keyword").skipDuplicates(true).size(10).fuzzy(ff -> ff.fuzziness("auto"))))
                    .suggesters("suggestionkeywordPinyin", sf -> sf.prefix(keyword).completion(c -> c.field("keywordPinyin").skipDuplicates(true).size(10).fuzzy(ff -> ff.fuzziness("auto"))))
                    .suggesters("suggestionkeywordSequence", sf -> sf.prefix(keyword).completion(c -> c.field("keywordSequence").skipDuplicates(true).size(10).fuzzy(ff -> ff.fuzziness("auto"))))), SuggestIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> keywordList = new ArrayList<>();
        keywordList.addAll(getSuggestResult(searchResponse, "suggestionKeyword"));
        keywordList.addAll(getSuggestResult(searchResponse, "suggestionkeywordPinyin"));
        keywordList.addAll(getSuggestResult(searchResponse, "suggestionkeywordSequence"));
        return keywordList;
    }

    private List<String> getSuggestResult(SearchResponse<SuggestIndex> searchResponse, String suggestionKeywordType) {
        List<Suggestion<SuggestIndex>> suggestions = searchResponse.suggest().get(suggestionKeywordType);
        List<String> stringList = suggestions.get(0).completion().options().stream().map(option -> {
            assert option.source() != null;
            return option.source().getTitle();
        }).toList();
        return stringList;
    }

    private AlbumSearchResponseVo parseResultData(SearchResponse<AlbumInfoIndex> searchResponse) {
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        searchResponse.hits().hits().forEach(hit -> {
            AlbumInfoIndex albumInfoIndex = hit.source();
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            if (null != albumInfoIndex) {
                BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
                // 判断是否通过关键词查询 --> 高亮
                if (null != hit.highlight().get("albumTitle") && StringUtils.isNotBlank(hit.highlight().get("albumTitle").get(0))) {
                    albumInfoIndexVo.setAlbumTitle(hit.highlight().get("albumTitle").get(0));
                }
                // 以下高亮不生效：怀疑是前端的问题
                // if (null != hit.highlight().get("albumIntro") && StringUtils.isNotBlank(hit.highlight().get("albumIntro").get(0))) {
                //     albumInfoIndexVo.setAlbumIntro(hit.highlight().get("albumIntro").get(0));
                // }
                albumSearchResponseVo.getList().add(albumInfoIndexVo);
            }
        });

        albumSearchResponseVo.setTotal(searchResponse.hits().total().value());

        return albumSearchResponseVo;
    }

    private SearchRequest queryBuildDsl(AlbumIndexQuery albumIndexQuery) {
        // 创建 SearchRequest 对象，设置查询所需要的条件
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index("albuminfo");
        // 判断当前用户是通过哪种方式进行检索的。
        // 1. 关键字检索
        if (StringUtils.isNotBlank(albumIndexQuery.getKeyword())) {
            // query--bool--should--match
            searchRequestBuilder.query(q -> q.bool(b -> b.should(s -> s.match(m -> m.field("albumTitle").query(albumIndexQuery.getKeyword()))).should(s -> s.match(m -> m.field("albumIntro").query(albumIndexQuery.getKeyword())))));
            // 设置高亮 无检索不高亮
            searchRequestBuilder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<span style=color:#f86442>").postTags("</span>")).fields("albumIntro", hf -> hf.preTags("<span style=\"color:#f86442\">").postTags("</span>")));
        }
        // 2. 分类检索
        if (null != albumIndexQuery.getCategory1Id()) {
            searchRequestBuilder.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())))));
        }
        if (null != albumIndexQuery.getCategory2Id()) {
            searchRequestBuilder.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())))));
        }
        if (null != albumIndexQuery.getCategory3Id()) {
            searchRequestBuilder.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())))));
        }
        // 3. 属性检索
        List<String> attributeList = albumIndexQuery.getAttributeList();
        // 属性（属性id:属性值id）
        if (!CollectionUtils.isEmpty(attributeList)) {
            for (String attribute : attributeList) {
                String[] split = attribute.split(":");
                if (null != split && split.length == 2) {
                    // query-nested-path-query-bool-filter-term
                    // 多个 filter 条件，它们之间是逻辑与（AND）关系。
                    // 只通过属性与属性值查询
                    // searchRequestBuilder.query(q->q.nested(n->n.path("attributeValueIndexList").query(nq->nq.bool(nb->nb.filter(f->f.term(t->t.field("attributeValueIndexList.attributeId").value(split[0]))).filter(f->f.term(t->t.field("attributeValueIndexList.valueId").value(split[1])))))));
                    // 有其他查询或过滤条件时-->多条件查询
                    // 这里使用 filter 与前述关键字检索以及分类检索构成逻辑与关系
                    searchRequestBuilder.query(q -> q.bool(b -> b.filter(f -> f.nested(n -> n.path("attributeValueIndexList").query(nq -> nq.bool(nb -> nb.filter(f1 -> f1.term(t1 -> t1.field("attributeValueIndexList.attributeId").value(split[0]))).filter(f2 -> f2.term(t2 -> t2.field("attributeValueIndexList.valueId").value(split[1])))))))));
                }
            }
        }
        // 4. 排序
        // 4.1 获取排序方式
        // 排序（综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]；asc:升序 desc:降序）
        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            String[] split = order.split(":");
            // 声明一个排序字段
            String orderField = "hotScore"; // 默认按照 hotScore 排序
            switch (split[0]) {
                case "2":
                    orderField = "playStatNum";
                    break;
                case "3":
                    orderField = "createTime";
                    break;
            }
            // 4.2 设置排序字段与规则，构建 DSL
            String finalOrderField = orderField; // finalOrderField 是 “实质上 final”
            searchRequestBuilder.sort(s -> s.field(f -> f.field(finalOrderField).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc))); // 默认降序
        }
        // 5. 分页
        Integer from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        searchRequestBuilder.from(from);
        searchRequestBuilder.size(albumIndexQuery.getPageSize());
        // SearchRequest 用于封装 DSL 语句
        SearchRequest searchRequest = searchRequestBuilder.build();
        System.out.println("DSL:\t" + searchRequest);
        return searchRequest;
    }
}
