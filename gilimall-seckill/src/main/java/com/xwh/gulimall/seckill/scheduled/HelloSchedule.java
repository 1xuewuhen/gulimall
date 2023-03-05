package com.xwh.gulimall.seckill.scheduled;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 *      1、@EnableScheduling 开启定时任务
 *      2、@Scheduled(cron = "1 * * * * ?")
 *      3、自动配置类TaskSchedulingAutoConfiguration
 *
 *
 * 异步任务
 *      1、@EnableAsync 开启异步任务
 *      2、@Async 给希望异步执行的方法加上
 *      3、自动配置类TaskExecutionAutoConfiguration 属性绑定在TaskExecutionProperties
 */

//@Slf4j
//@EnableScheduling
//@EnableAsync
//@Component
public class HelloSchedule {

    /**
     * spring 中只允许6位组成，不允许7位
     * spring 中 周就是1-7 星期1-星期7
     * 定时任务不应该阻塞，默认是阻塞的
     *      1、异步方式执行，自己提交到线程池
     *      2、支持定时任务线程池：TaskSchedulingProperties
     *          有些版本当中不好使spring.task.scheduling.pool.size=10
     *      3、让定时任务异步执行
     */
//    @Async
//    @Scheduled(cron = "*/1 * * ? * 6")
//    public void hello() throws InterruptedException {
//        log.info("hello...");
//        Thread.sleep(3000);
//    }

}
