package com.xwh.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.xwh.gulimall.product.entity.ProductAttrValueEntity;
import com.xwh.gulimall.product.service.ProductAttrValueService;
import com.xwh.gulimall.product.vo.AttrRespVo;
import com.xwh.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.xwh.gulimall.product.entity.AttrEntity;
import com.xwh.gulimall.product.service.AttrService;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.R;


/**
 * 商品属性
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 11:14:32
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    /**
     * /product/attr/base/listforspu/{spuId}
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrListForSpu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entities = productAttrValueService.baseAttrListForSpu(spuId);
        return R.ok().put("data",entities);
    }

    /**
     * 列表
     * /product/attr/base/list/{catelogId}
     * /product/attr/sale/list/{catelogId}
     */
    @RequestMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String type) {
        PageUtils page = attrService.queryBaseAttrPage(params, catelogId,type);

        return R.ok().put("page", page);
    }

    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     * /product/attr/info/{attrId}
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
//        AttrEntity attr = attrService.getById(attrId);

        AttrRespVo attrRespVo = attrService.getAttrInfo(attrId);
        return R.ok().put("attr", attrRespVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr) {
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr) {
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * /product/attr/update/{spuId}
     */
    @RequestMapping(value = "/update/{spuId}",method = RequestMethod.POST)
    public R updateSpuAttr(@RequestBody List<ProductAttrValueEntity> entities,@PathVariable("spuId") Long spuId) {
        productAttrValueService.updateSpuAttr(spuId,entities);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds) {
        attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
