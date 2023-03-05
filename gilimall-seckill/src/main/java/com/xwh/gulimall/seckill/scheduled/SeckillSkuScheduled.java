package com.xwh.gulimall.seckill.scheduled;


import com.xwh.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点上架，上架最近三天需要秒杀的商品
 * 当天00:00:00-23:59:59
 * 明天00:00:00-23:59:59
 * 后天00:00:00-23:59:59
 */
@Service
@Slf4j
public class SeckillSkuScheduled {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    @Async
    //TODO 幂等设计
    @Scheduled(cron = "0 0/1 * * * ?")
    public void uploadSeckillSkuLatest() {
        // 1、重复上架无需处理
        log.info("商品上架");
//        upload_lock
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();

        } finally {
            lock.unlock();
        }
    }
}
