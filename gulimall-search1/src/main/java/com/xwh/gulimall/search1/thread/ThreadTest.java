package com.xwh.gulimall.search1.thread;

import java.util.concurrent.*;

public class ThreadTest {

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5,
            10,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(20),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main线程结束");
//        new Thread01().start();
//        new Thread(new Runbale01()).start();
//        FutureTask<Integer> task = new FutureTask<>(new Callable01());
//        new Thread(task).start();
//        Integer integer = task.get();
//        System.out.println(integer);

        /**
         * int corePoolSize,
         * int maximumPoolSize,
         * long keepAliveTime,
         * TimeUnit unit,
         * BlockingQueue<Runnable> workQueue,
         * ThreadFactory threadFactory,
         * RejectedExecutionHandler handler
         */

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("开那是");
            int i = 10 / 2;
            System.out.println("结束");
            return i;
        }, executor).handle((r, t) -> {
            if (r != null) {
                return r;
            }
            if (t != null) {
                return 100;
            }
            return 0;
        }).thenApplyAsync((u)->{
            System.out.println(u);
            return 10;
        },executor);
        Integer integer = future.get();

        System.out.println("main线程开始"+integer);
    }

    public static class Thread01 extends Thread {

        @Override
        public void run() {
            System.out.println("Thread01" + Thread.currentThread().getId());
            System.out.println(10 / 3);
        }
    }

    public static class Runbale01 implements Runnable {
        @Override
        public void run() {
            System.out.println("Runbale01" + Thread.currentThread().getId());
            System.out.println(10 / 3);
        }
    }

    public static class Callable01 implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Callable01" + Thread.currentThread().getId());
            int i = 10 / 3;
            System.out.println(i);
            return i;
        }
    }


}
