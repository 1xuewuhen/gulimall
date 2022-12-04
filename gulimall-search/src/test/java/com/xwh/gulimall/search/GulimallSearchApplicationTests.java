/*
package com.xwh.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.xwh.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;


    @Test
    void contextLoads() {
        System.out.println(client);
    }


    */
/**
     * 测试存储数据到 es
     * source 方法用于保存数据，数据的格式为键值对形式的类型
     * - json 字符串
     * - Map
     * - XContentBuilder
     * - KV 键值对
     * - 实体类对象转json
     *//*

    @Test
    void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUsername("张三");
        user.setGender("女");
        user.setAge(18);
        indexRequest.source(JSON.toJSONString(user), XContentType.JSON);
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(index);

    }


    @Data
    static class User {
        private String username;
        private String gender;
        private Integer age;
    }

}
*/
