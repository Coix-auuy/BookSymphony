package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Author HeZx
 * Time 2024/11/11 15:27
 * Version 1.0
 * Description:
 */
/*
在 Spring Data Elasticsearch 中，如果你定义了一个接口并扩展了 ElasticsearchRepository，Spring 会自动管理这个接口的实现（自动实现接口），并将其注册为一个 bean。
因此，即使没有显式添加任何注解，Spring 也会自动管理 AlbumIndexRepository。
*/
public interface AlbumIndexRepository extends ElasticsearchRepository<AlbumInfoIndex, Long> {

}
