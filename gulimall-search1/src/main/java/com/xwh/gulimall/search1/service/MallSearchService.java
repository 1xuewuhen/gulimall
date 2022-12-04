package com.xwh.gulimall.search1.service;

import com.xwh.gulimall.search1.vo.SearchParam;
import com.xwh.gulimall.search1.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
