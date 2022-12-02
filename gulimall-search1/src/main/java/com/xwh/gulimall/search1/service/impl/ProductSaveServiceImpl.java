package com.xwh.gulimall.search1.service.impl;

import com.alibaba.fastjson.JSON;
import com.xwh.common.to.es.SkuEsModel;
import com.xwh.gulimall.search1.config.GulimallElasticSearchConfig;
import com.xwh.gulimall.search1.constant.EsConstant;
import com.xwh.gulimall.search1.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
/*    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        return false;
    }*/

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        // 给es建立索引，product 建立映射关系

        // 给es中保存这些数据
        // BulkRequest bulkRequest, RequestOptions options
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        //TODO 如果批量错误
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
        log.info("商品上架成功:{}，返回数据{}", collect,bulk.toString());

        return b;
    }
}
