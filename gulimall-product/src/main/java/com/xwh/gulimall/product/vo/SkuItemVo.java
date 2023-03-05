package com.xwh.gulimall.product.vo;


import com.xwh.gulimall.product.entity.SkuImagesEntity;
import com.xwh.gulimall.product.entity.SkuInfoEntity;
import com.xwh.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    private SkuInfoEntity info;
    private Boolean hasStock = true;
    private List<SkuImagesEntity> images;
    private List<SkuItemSaleAttrVo> saleAttr;
    private SpuInfoDescEntity desc;
    private List<SpuItemAttrGroupVo> groupAttrs;
    private SeckillInfoVo seckillInfo;

    @Data
    public static class SkuItemSaleAttrVo {
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @Data
    public static class SpuItemAttrGroupVo {
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @Data
    public static class SpuBaseAttrVo {
        private String attrName;
        private String attrValue;
    }

    @Data
    public static class AttrValueWithSkuIdVo{
        private String attrValue;
        private String skuIds;
    }
}
