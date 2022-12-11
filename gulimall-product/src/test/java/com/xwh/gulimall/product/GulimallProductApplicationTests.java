package com.xwh.gulimall.product;

import com.xwh.gulimall.product.service.AttrGroupService;
import com.xwh.gulimall.product.service.SkuSaleAttrValueService;
import com.xwh.gulimall.product.vo.SkuItemVo;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

@SpringBootTest
class GulimallProductApplicationTests {


    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Test
    public void test01() {
//        System.out.println(stringRedisTemplate);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
//        ops.set("hello","world",2, TimeUnit.MINUTES);
        System.out.println(ops.get("hello"));
    }

    @Test
    public void test02() {
        int round = (int) (Math.random() * 10 + 1);
        System.out.println(round);
    }

    @Test
    public void test03(){
        System.out.println(redissonClient);
    }


    @Test
    public void test04(){
        List<SkuItemVo.SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupService.getAttrGroupWithAttrsBySpuId(11L, 225L);

        System.out.println(attrGroupWithAttrsBySpuId);
    }

    @Test
    public void test05(){
        List<SkuItemVo.SkuItemSaleAttrVo> saleAttrBySpuId = skuSaleAttrValueService.getSaleAttrBySpuId(12L);
        System.out.println(saleAttrBySpuId);
    }
}
