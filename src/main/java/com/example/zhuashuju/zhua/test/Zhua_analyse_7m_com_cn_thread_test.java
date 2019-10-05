package com.example.zhuashuju.zhua.test;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * http://chromedriver.storage.googleapis.com/index.html 这个url坑死了，里面对应的版本都不对
 * http://npm.taobao.org/mirrors/chromedriver/ 找到对应版本的 dirver
 * ChromeDriver 仓库
 * 自己电脑装的 chrome 版本是 V76
 */
public class Zhua_analyse_7m_com_cn_thread_test {
    private static Integer threadPoolSize = 8;
    public static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {

        test1();


    }

    public static void test1 () {
        // 创建数量为10的固定线程池
        /*ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
        // 计数器，等待所有线程执行完成
        CountDownLatch cdAnswer = null;

        List<Integer> listTest = new ArrayList<>();
        for (int i = 0; i <1000 ; i++) {
            listTest.add(i);
        }

        // 每个线程处理多少个 WebElement
        Integer weCount = listTest.size() / threadPoolSize;

        Integer yushu = listTest.size() % threadPoolSize;

        // 剩下最后一个线程，特殊处理（加余数）
        for (int i = 0; i < threadPoolSize - 1 ; i++) {
            List<Integer> alist;
            if (i < threadPoolSize - 1) {
                alist = listTest.subList(i* weCount, (i+1) *weCount);

            } else {
                alist = listTest.subList(i* weCount, (i+1) *weCount + yushu);
            }
            threadPool.execute(new MyThread(alist));
        }

        try {
            cdAnswer.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        threadPool.shutdown();*/

    }

    public static class MyThread implements Runnable {
        private List<Integer> alist;

        public MyThread(List<Integer> alist) {
            this.alist = alist;
        }

        @Override
        public void run() {
            for (int i = 0; i < alist.size(); i++) {
                System.out.println(alist.get(i));
            }
        }
    }
}
