package com.atguigu.gmall.cart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

@SpringBootTest
class GmallCartApplicationTests {

    @Test
    void contextLoads() throws ExecutionException, InterruptedException {


       ThreadLocal<String> threadLocal = new ThreadLocal<>();

        System.out.println(threadLocal);

       new Thread(){
           @Override
           public void run() {
               threadLocal.set("局部变量1");   //k v    loal
               threadLocal.set("局部变量2");  //会覆盖
               System.out.println(Thread.currentThread().getName());
               System.out.println("threadLocal--------------"+threadLocal.get());
//               System.out.println(threadLocal.get());
//               int a=1/0;
           }
       }.start();


       new Thread(new Runnable() {
           @Override
           public void run() {
               threadLocal.set("局部变量2");
//               System.out.println("线程二");
               System.out.println(Thread.currentThread().getName());
               System.out.println("threadLocal--------------"+threadLocal.get());
           }
       }).start();



        FutureTask futureTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() {
                threadLocal.set("局部变量3");
                System.out.println(Thread.currentThread().getName());
                System.out.println("threadLocal--------------"+threadLocal.get());
//                int i=1/0;
                return "你好";
            }
        });
        Thread thread = new Thread(futureTask);
        thread.start();
        System.out.println("返回值="+futureTask.get());
        System.out.println("子线程状态="+futureTask.isDone());



//   核心线程数 4,最大扩展线程数6,线程存活时间1000,单位，数组结构有界阻塞队列， 默认拒绝策略抛出异常
//        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
//                4,6, 1000
//                ,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.AbortPolicy());
//
//        poolExecutor.execute(() -> {
//            System.out.println(Thread.currentThread().getName());
//        });
//        poolExecutor.execute(() -> {
//            System.out.println(Thread.currentThread().getName());
//        });
//        poolExecutor.execute(() -> {
//            System.out.println(Thread.currentThread().getName());
//        });
    }

}
