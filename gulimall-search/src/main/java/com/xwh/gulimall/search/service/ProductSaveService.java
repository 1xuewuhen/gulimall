package com.xwh.gulimall.search.service;

import com.xwh.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {


    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
