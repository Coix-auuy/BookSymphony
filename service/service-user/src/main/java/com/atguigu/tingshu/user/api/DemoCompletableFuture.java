package com.atguigu.tingshu.user.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Author HeZx
 * Time 2024/10/25 18:02
 * Version 1.0
 * Description:
 */
public class DemoCompletableFuture {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 无返回值
        // CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> System.out.println("hello"));
        // future1.get(): get() 方法获取线程执行完的结果。
        // System.out.println(future1.get());
        // 有返回值
        // CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
        //     int total = 0;
        //     for (int i = 1; i <= 365; i++) {
        //         total += i;
        //     }
        //     // total = 1 / 0;
        //     return total;
        // }).whenComplete((t, u) -> {
        //     // t -- 返回值
        //     System.out.println(t);
        //     // u -- 线程执行中抛出的异常
        //     System.out.println(u);
        // });
        // System.out.println(future2.get());

        CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> {
            int total = 0;
            for (int i = 1; i <= 365; i++) {
                total += i;
            }
            // total = 1 / 0;
            return total;
        }).thenApply(t ->{
            return 2 * t;
        }).thenAccept(f ->{
            System.out.println(f);
        });
        System.out.println(future2.get());
    }
}
