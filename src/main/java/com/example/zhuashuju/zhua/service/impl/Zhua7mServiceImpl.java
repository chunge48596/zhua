package com.example.zhuashuju.zhua.service.impl;

import com.example.zhuashuju.zhua.excel.ExcelUtil;
import com.example.zhuashuju.zhua.service.Zhua7mService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * // 根据 HTML元素名称(tr td a input div)找元素
 * webElement.findElements(By.tagName("tr"))
 * // 根据 元素 id 找元素集合
 * webElement.findElements(By.id("aaa"))
 * // 根据 元素 name 找元素集合
 * webElement.findElements(By.name("aaa"))
 * // 根据 元素 class 找元素集合
 * webElement.findElements(By.className("aaa"))
 * // 获取 元素 属性值
 * webElement.getAttribute("value")
 * // 获取 元素 文本值
 * webElement.getText()
 * // 触发 webElement 的 js，driver 为当前加载完全的页面
 * ((JavascriptExecutor) driver).executeScript("showTS('B',1)");
 * // 调试，如果当前webElement 获取当前元素的 html
 * .getAttribute("outerHTML")
 * <p>
 * // WebDriver判定isDisplayed为false的元素,那么getText()将为空
 * // isDisplayed为false的元素,依然可以通过.getAttribute("innerHTML")方法获取元素的属性.
 */
@Service
public class Zhua7mServiceImpl implements Zhua7mService {
    private static final Logger log = LoggerFactory.getLogger(Zhua7mServiceImpl.class);

    @Value("${webdriver.path}")
    private String webdriverPath;
    @Value("${file.path}")
    private String filePath;

    // 线程池数量
    private static Integer threadPoolSize = 8;
    // 线程安全，自增比赛场次
    public static AtomicInteger count = null;
    // 当天比赛数量
    public static Integer gameCount = null;
    // 规则1 最终结果
    private static StringBuffer result = null;
    // 规则2 最终结果
    private static StringBuffer result2 = null;
    // WebDriver 池
    private static List<WebDriver> listWd = new ArrayList<>(threadPoolSize);
    // 时间格式
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    // 文件名
    private static String fileName = null;
    // 带路径文件名
    private static String pathFileName = null;
    // 当天剩余抓取次数
    private static Integer zhuaCount = 3;
    // 是否存在计时器
    private static boolean isTime = false;
    // 锁
    public static Lock lock = new ReentrantLock();
    // 密码
    private static String PASSWORD = "";

    @Override
    public String zhua(Integer guiZe) {
        Long beginTime = System.currentTimeMillis();
        count = new AtomicInteger(0);
        result = new StringBuffer();
        gameCount = 0;

        // 第一个WebDriver 还是自己创建自己的
        WebDriver webDriver = getWebDriver("");
        webDriver.get("http://bf.7m.com.cn/default_split_gb.aspx?view=all&match=&line=no");
        // 找到主页HTML
        WebElement webElement = webDriver.findElement(By.xpath("/html"));
        // 创建 WebDriver 池，用于线程使用
        createWebDriverPool(threadPoolSize);
        // 创建固定线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
        // 计数器，等待所有线程执行完成
        CountDownLatch cdAnswer = new CountDownLatch(threadPoolSize);

        /**
         * 找到两个比赛table列表
         * <table class="live" cellpadding="2" cellspacing="1" id="left_live_Table">
         * <table class="live" cellpadding="2" cellspacing="1" id="right_live_Table">
         */
        List<WebElement> liveTables = webElement.findElements(By.className("live"));

        // 所有场次比赛tr
        List<WebElement> trList = new ArrayList<>();
        // 先统计出所有场次比赛tr，后续就不用再循环 liveTables 了
        for (WebElement tableWe1 : liveTables) {
            List<WebElement> tbg0 = tableWe1.findElements(By.className("tbg0"));
            trList.addAll(tbg0);
            List<WebElement> tbg1 = tableWe1.findElements(By.className("tbg1"));
            trList.addAll(tbg1);
        }
        // todo 测试用
        //trList = trList.subList(0, 5);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        System.out.println("========================== " + sdf.format(new Date()) + " 共: " + trList.size() + " 场比赛==========================");
        // log.info("========================== " + sdf.format(new Date()) + " 共: " + trList.size() + " 场比赛==========================");
        gameCount = trList.size();

        try {
            // 获取所有 WebElement 长度
            Integer weSize = trList.size();

            // 每个线程处理多少个 WebElement
            Integer weCount = weSize / threadPoolSize;

            // 余数
            Integer lastCount = weSize % threadPoolSize;

            for (int i = 0; i < threadPoolSize; i++) {
                List<WebElement> threadWeList;
                if (i < threadPoolSize - 1) { // 排除线程池最后一个线程
                    threadWeList = trList.subList(i * weCount, (i + 1) * weCount);

                } else { // 线程池最后一个线程，需要加 lastCount 余数
                    threadWeList = trList.subList(i * weCount, (i + 1) * weCount + lastCount);
                }
                // 执行线程
                threadPool.execute(new MyThread(cdAnswer, threadWeList, guiZe));
            }
            try {
                // 等待所有线程执行完毕，这里必须等待，不然会直接走到下面的，关闭线程池！！！！！
                cdAnswer.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("==========================规则" + guiZe + "统计结果==========================");
            System.out.println(result.toString());
            System.out.println("==========================规则" + guiZe + "排序结果==========================");
            result = orderByTimeAsc(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Long time1 = System.currentTimeMillis();
            /**
             * 关闭driver quit 表示关闭所有窗口和资源，close 表示关闭当前窗口
             * 这里只关闭主页WebDriver, driver 池不关闭，下次请求直接用，会提升效率
             */
            webDriver.quit();
            // 关闭线程池
            threadPool.shutdown();
            // 销毁 WebDriver 池
            closeWebDriverPool();
            // Long time2 = System.currentTimeMillis();
            //System.out.println("finally 关闭线程池，销毁webDriver池，共消耗时间" + (time2 - time1));
            System.out.println("===========程序退出===============");
        }
        Long endTime = System.currentTimeMillis();
        System.out.println("总耗时：" + (endTime - beginTime));
        return result.toString();
    }

    /**
     * 排序
     */
    private StringBuffer orderByTimeAsc(StringBuffer result) {
        String order = result.toString();
        String[] resultArray = order.split(",");
        List<Map<String, String>> listMap = new ArrayList<>();
        for (int i = 0; i < resultArray.length; i++) {
            Map<String, String> map = new HashMap<>();
            String time = "";
            try {
                time = resultArray[i].split(">")[1].trim();
            } catch (Exception e) {
                System.out.println(resultArray[i]);
            }
            map.put(time, resultArray[i]);
            listMap.add(map);
        }

        // 排序
        Collections.sort(listMap, (o1, o2) -> o1.keySet().iterator().next().compareToIgnoreCase(o2.keySet().iterator().next()));

        StringBuffer softResult = new StringBuffer();
        for (int i = 0; i < listMap.size(); i++) {
            Map<String, String> map = listMap.get(i);
            for (Map.Entry<String, String> m : map.entrySet()) {
                softResult.append(m.getValue()).append("\r\n");
            }
        }
        System.out.println(softResult.toString());
        return softResult;
    }

    /**
     * 获取web驱动
     *
     * @param url
     * @return
     */
    public WebDriver getWebDriver(String url) {

        ChromeOptions options = new ChromeOptions();
        // 设置chrome选项 不弹出网页
        options.addArguments("--headless");
        // 设置chrome选项 禁止gpu
        options.addArguments("--disable-gpu");
        // 关闭使用ChromeDriver打开浏览器时上部提示语"Chrome正在受到自动软件的控制"
        options.addArguments("disable-infobars");
        // 禁用插件
        options.addArguments("--disable-plugins");
        // 禁止js
        options.addArguments("--disable-javascript");
        // 设置禁止加载项
        Map<String, Object> prefs = new HashMap<>();
        // 禁止加载js
        prefs.put("profile.managed_default_content_settings.javascript", 2); // 2就是代表禁止加载的意思
        // 禁止加载图片
        prefs.put("profile.managed_default_content_settings.images", 2); // 2就是代表禁止加载的意思
        options.setExperimentalOption("prefs", prefs);

        // webdriverPath todo 这里不用指定 webDriver 也能用，不清楚为啥啊
        System.getProperties().setProperty("webdriver.chrome.driver",
                webdriverPath);

        // 这里不要每次都new ChromeDriver 太耗费资源
        // 这里不存在多线程并发问题，因为所有的线程，都是这一套启动参数
        // 去掉线程中，finally 关闭dirver
        WebDriver driver = new ChromeDriver(options);
        return driver;
    }

    /**
     * 创建 webDriver 池，用于多线程，不频繁创建销毁webDriver,减少资源消耗
     *
     * @param poolSize
     * @return
     */
    private void createWebDriverPool(Integer poolSize) {
        if (CollectionUtils.isEmpty(listWd)) {
            for (int i = 0; i < poolSize; i++) {
                listWd.add(this.getWebDriver(""));
            }
        }
    }

    /**
     * 根据 List<WebDriver> 下标，每个线程各自的 dirver
     *
     * @param index
     */
    private WebDriver getWebDriverByPoolIndex(int index) {
        Integer ind = index % threadPoolSize;
        // System.out.println(String.format("线程ID:%s=========线程下标:%s", index, ind));
        return listWd.get(ind);
    }

    /**
     * 销毁 webDriver 池
     */
    private void closeWebDriverPool() {
        for (int i = 0; i < listWd.size(); i++) {
            listWd.get(i).quit();
        }
        listWd.clear();
    }

    private String getScore(WebElement webElement, String className) {
        return webElement.findElement(By.id(className)) // 找到主场所有场次table
                .findElements(By.tagName("tr")).get(2) // 找到 第三行，首场比赛 tr
                .findElement(By.className("td_score")) // 找到 td
                .findElement(By.tagName("a")).getText() // 获取 a 标签的文本
                .replace("<span>", "").replace("</span>", ""); // 替换掉没用元素
    }


    public class MyThread implements Runnable {
        private List<WebElement> weList;
        private CountDownLatch cdAnswer;
        private Integer guiZe;

        public MyThread(CountDownLatch cdAnswer, List<WebElement> weList, Integer guiZe) {
            this.cdAnswer = cdAnswer;
            this.weList = weList;
            this.guiZe = guiZe;
        }

        @Override
        public void run() {
            for (int i = 0, size = weList.size(); i < size; i++) {
                try {
                    // 先判断是否是比赛的tr <input type="checkbox" name="hiddenChk" id="hiddenChk3946398" value="3946398" onclick="javascript:showselMdiv(this)">
                    // 取不到元素，直接抛出异常了
                    WebElement input;
                    try {
                        input = weList.get(i).findElement(By.name("hiddenChk"));
                    } catch (Exception e) {
                        continue;
                    }

                    if (input != null) { // 有比赛
                        // 找到tr行中 完 用于判断是否需要这个比赛
                        String complete = weList.get(i).findElement(By.className("state")).getText();
                        if (complete.equalsIgnoreCase("完")) {
                            continue;
                        }

                        // 赛事
                        String sai = weList.get(i).findElement(By.className("match")).getText();

                        // 编号
                        String bianhao = input.getAttribute("value");

                        // 比赛时间 只获取没有隐藏的数据
                        String gameTime = weList.get(i).findElement(By.className("time")).getText();

                        // 找到tr行中主队
                        String zhuDui = weList.get(i).findElements(By.tagName("a")).get(0).getText();

                        // 找到tr行中客队
                        String keDui = weList.get(i).findElements(By.tagName("a")).get(2).getText();


                        //System.out.println("===========================比赛队伍：" + zhuDui + " VS " + keDui + "编号：" + bianhao);

                        System.out.println("线程:" + Thread.currentThread().getId() % threadPoolSize + " 正在抓取," + zhuDui + " VS " + keDui);
                        // log.info("线程:" + Thread.currentThread().getId() % threadPoolSize + " 正在抓取," + zhuDui + " VS " + keDui);

                        // 找出所有符合规则的数据
                        List<Boolean> list = isZoneZone(bianhao, zhuDui, keDui, guiZe);
                        if (list.get(0)) {
                            // 成功,打印对应规则信息
                            result.append(sai + " > " + gameTime + " > " + zhuDui + " VS " + keDui + ",");
                            //System.out.println("规则" + guiZe + " " + gameTime + ": " + zhuDui + " VS " + keDui);
                        }
                    }
                } finally {
                    // 统计查了多少场比赛了,放在这里，是因为必须是当前线程已经统计完了，才增加数量
                    count.incrementAndGet();
                    System.out.println(count);
                }
            }
            // 草他妈的，这里忘记了(每个线程执行完毕，必须countDown())，导致外层的计数器cdAnswer 一直在等待
            cdAnswer.countDown();
        }
    }

    /**
     * 按照规则，找0-0比赛
     *
     * @param bianHao 编号
     * @param zhuDui  主队
     * @param keDui   客队
     * @param guiZe   规则
     * @return
     */
    public List<Boolean> isZoneZone(String bianHao, String zhuDui, String keDui, Integer guiZe) {
        List<Boolean> listBoolean = new ArrayList<>();

        // 析 URL
        StringBuffer url = new StringBuffer("http://analyse.7m.com.cn/");
        url.append(bianHao).append("/index_gb.shtml");
        WebDriver driver = getWebDriverByPoolIndex((int) Thread.currentThread().getId());
        try {
            driver.get(url.toString());
        } catch (Exception e) {
            System.out.println("线程:" + Thread.currentThread().getId() % threadPoolSize + " 异常," + zhuDui + " VS " + keDui + ",异常信息:" + e);
            // log.info("线程:" + Thread.currentThread().getId() % threadPoolSize + " 异常," + zhuDui + " VS " + keDui + ",异常信息:", e);
        }

        WebElement webElement = driver.findElement(By.xpath("/html"));
        String zhuKe = zhuDui + " VS " + keDui;

        if (guiZe == 1) {
            /******************************************* 规则1 ***********************************************/
            boolean booleanGuiZe1 = getGuiZe1(driver, webElement, zhuKe);
            listBoolean.add(booleanGuiZe1);
        } else if (guiZe == 2) {
            /******************************************* 规则2 ***********************************************/
            boolean booleanGuiZe2 = false;
            int count = getGuiZe2_optimize(driver, webElement, zhuKe);
            // 现在只要不存在0:0的比赛，只需要把统计的结果判断是否是0
            if (count == 0) {
                booleanGuiZe2 = true;
            }
            listBoolean.add(booleanGuiZe2);
        }

        // 返回规则
        return listBoolean;
    }

    /**
     * 规则1
     * 1、主场总场数，有没有0:0
     * 2、主场，有没有0:0
     * 3、客场总场数，有没有0:0
     * 4、客场，有没有0:0
     *
     * @param driver
     * @param webElement
     * @param zhuKe
     * @return
     */
    private boolean getGuiZe1(WebDriver driver, WebElement webElement, String zhuKe) {
        // 主场all eg:0:0
        String homeAllScore;
        try {
            homeAllScore = getScore(webElement, "tbTeamHistory_A_all");
            // 获得主场所有场次比分
            if (homeAllScore.equals("0-0")) {
                return true;
            }
            //System.out.println(zhuKe + " 主场所有场次>: " + homeAllScore);

        } catch (Exception e) {
            System.out.println(zhuKe + " 主场所有场次>未找到比赛记录");
            // log.info(zhuKe + " 主场所有场次>未找到比赛记录");
            //e.printStackTrace();
        }

        // 主场home
        String homeScore;
        try {
            // 执行 js 方法，显示主场比赛
            ((JavascriptExecutor) driver).executeScript("showTS('A',1)");
            // 获得主场比分
            homeScore = getScore(webElement, "tbTeamHistory_A_home");
            if (homeScore.equals("0-0")) {
                return true;
            }
            //System.out.println(zhuKe + " 主场>: " + homeScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 主场>未找到比赛记录");
            // log.info(zhuKe + " 主场>未找到比赛记录");
        }

        // 客场all
        String awayAllScore;
        try {
            // 获得客场比分
            awayAllScore = getScore(webElement, "tbTeamHistory_B_all");
            if (awayAllScore.equals("0-0")) {
                return true;
            }
            //System.out.println(zhuKe + " 客场总场次>: " + awayAllScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 客场总场次>未找到比赛记录");
            // log.info(zhuKe + " 客场总场次>未找到比赛记录");
        }

        // 客场away
        String awayScore;
        try {
            // 执行 js 方法，显示客场比赛
            ((JavascriptExecutor) driver).executeScript("showTS('B',1)");
            // 获得客场比分
            awayScore = getScore(webElement, "tbTeamHistory_B_away");
            if (awayScore.equals("0-0")) {
                return true;
            }
            //System.out.println(zhuKe + " 客场>: " + awayScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 客场>未找到比赛记录");
            // log.info(zhuKe + " 客场>未找到比赛记录");
        }
        return false;
    }

    /**
     * 规则2，优化版
     * 1、主场、客场场数，总场数，不存在0:0
     * 2、客场总场数 和 客场，不存在0:0
     * 3、球会友谊的数据排除掉
     * 4、只要当年的数据
     *
     * @param driver
     * @param webElement
     * @return
     */
    private int getGuiZe2_optimize(WebDriver driver, WebElement webElement, String zhuKe) {
        Long beginTime = System.currentTimeMillis();
        try {
            // 主场总场数所有 tr
            List<WebElement> listZhuChangAll = new ArrayList<>();
            try {
                /**
                 * https://www.cnblogs.com/yufeihlf/p/5717291.html#test8 这个网址有对 xpath 的介绍
                 * https://blog.csdn.net/u012941152/article/details/83011110  这个更加的实用
                 * ".//a[contains(text(),'0-0')]/../.." 下面是对这段表达式的解释
                 *  .  表示从当前对象下开始找，不加 . 会从跟节点找
                 *  // 表示从跟节点下找
                 *  a  表示要查找的节点元素
                 *  contains 表示包含
                 *  text(),'0-0' 表示文本 = 0-0
                 *  /.. 表示当前节点的父节点
                 *
                 *  By.xpath("..") 找到当前节点的父节点
                 */
                listZhuChangAll = webElement.findElement(By.id("tbTeamHistory_A_all"))
                        .findElements(By.xpath(".//a[contains(text(),'0-0')]/../.."));
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 主场所有场次>找0-0比赛记录异常" + e);
            }

            // 主场所有 tr
            List<WebElement> listZhuChang = new ArrayList<>();
            try {
                // 执行 js 方法，显示主场比赛
                ((JavascriptExecutor) driver).executeScript("showTS('A',1)");
                listZhuChang = webElement.findElement(By.id("tbTeamHistory_A_home"))
                        .findElements(By.xpath(".//a[contains(text(),'0-0')]/../.."));
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 主场>找0-0比赛记录异常" + e);
            }

            // 客场总场数所有 tr
            List<WebElement> listKeChangAll = new ArrayList<>();
            try {
                listKeChangAll = webElement.findElement(By.id("tbTeamHistory_B_all"))
                        .findElements(By.xpath(".//a[contains(text(),'0-0')]/../.."));
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 客场总场次>找0-0比赛记录异常" + e);
            }

            // 客场所有 tr
            List<WebElement> listKeChang = new ArrayList<>();
            try {
                // 执行 js 方法，显示客场比赛
                ((JavascriptExecutor) driver).executeScript("showTS('B',1)");
                listKeChang = webElement.findElement(By.id("tbTeamHistory_B_away"))
                        .findElements(By.xpath(".//a[contains(text(),'0-0')]/../.."));
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 客场>找0-0比赛记录异常" + e);
            }

            // 汇总
            listZhuChangAll.addAll(listZhuChang);
            listZhuChangAll.addAll(listKeChangAll);
            listZhuChangAll.addAll(listKeChang);

            Integer listSize = listZhuChangAll.size();

            // 筛选 tr
            listZhuChangAll = filterGuiZe2_optimize(listZhuChangAll, zhuKe);

            Long endTime = System.currentTimeMillis();
            System.out.println("规则2 " + zhuKe + ",共" + listSize + "场比赛，抓取数据消耗时间：" + (endTime - beginTime));
            return listZhuChangAll.size();
        } catch (Exception e) {
            System.out.println(zhuKe + " 规则2，抓取数据异常！！！！");
            return -1;
        }
    }

    /**
     * 过滤比赛，优化版
     * 1、不是“球会友谊”
     * 2、只要当年比赛
     *
     * @param list
     * @return
     */
    private List<WebElement> filterGuiZe2_optimize(List<WebElement> list, String zhuKe) {
        // 排除 赛事 “球会友谊”
        final String EXCLUDE_GAME_NAME = "球会友谊";

        List<WebElement> newList = new ArrayList<>();
        WebElement web;
        for (int i = 0, size = list.size(); i < size; i++) {
            try {
                web = list.get(i);
                // 排除不需要的tr(1、球会友谊 的比赛不要；2、只要当年的数据；)
                if (!isGameName(web, EXCLUDE_GAME_NAME, zhuKe) && isNowYear(web, zhuKe)) {
                    newList.add(list.get(i));
                }
            } catch (Exception e) {
                // 异常了说明，当前 tr 不是一个比赛的 tr
            }
        }
        return newList;
    }


    /**
     * 规则2
     * 1、主场、客场场数，总场数，中只存在的一场0:0，或不存在0:0
     * 2、客场总场数 和 客场，中只存在的一场0:0，或不存在0:0
     * 3、球会友谊的数据排除掉
     * 4、只要当年的数据
     *
     * @param driver
     * @param webElement
     * @return规则1
     */
    private int getGuiZe2(WebDriver driver, WebElement webElement, String zhuKe) {
        Long beginTime = System.currentTimeMillis();
        try {
            // 主场总场数所有 tr
            List<WebElement> listZhuChangAll = new ArrayList<>();
            try {
                listZhuChangAll = webElement.findElement(By.id("tbTeamHistory_A_all"))
                        .findElements(By.className("tr_l0"));
                try {
                    // 可能会存在一场比赛，就只有 tr_10 ，tr_l1 会找不到报错
                    listZhuChangAll.addAll(webElement.findElement(By.id("tbTeamHistory_A_all"))
                            .findElements(By.className("tr_l1")));
                } catch (Exception e) {
                    System.out.println("规则2 " + zhuKe + " 主场所有场次>只有一场比赛");
                }
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 主场所有场次>未找到比赛记录");
            }

            // 主场所有 tr
            List<WebElement> listZhuChang = new ArrayList<>();
            try {
                // 执行 js 方法，显示主场比赛
                ((JavascriptExecutor) driver).executeScript("showTS('A',1)");
                listZhuChang = webElement.findElement(By.id("tbTeamHistory_A_home"))
                        .findElements(By.className("tr_l0"));
                try {
                    // 可能会存在一场比赛，就只有 tr_10 ，tr_l1 会找不到报错
                    listZhuChang.addAll(webElement.findElement(By.id("tbTeamHistory_A_home"))
                            .findElements(By.className("tr_l1")));
                } catch (Exception e) {
                    System.out.println("规则2 " + zhuKe + " 主场>只有一场比赛");
                }
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 主场>未找到比赛记录");
            }

            // 客场总场数所有 tr
            List<WebElement> listKeChangAll = new ArrayList<>();
            try {
                listKeChangAll = webElement.findElement(By.id("tbTeamHistory_B_all"))
                        .findElements(By.className("tr_l2"));
                try {
                    // 可能会存在一场比赛，就只有 tr_10 ，tr_l1 会找不到报错
                    listKeChangAll.addAll(webElement.findElement(By.id("tbTeamHistory_B_all"))
                            .findElements(By.className("tr_l3")));
                } catch (Exception e) {
                    System.out.println("规则2 " + zhuKe + " 客场总场次>只有一场比赛");
                }
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 客场总场次>未找到比赛记录");
            }

            // 客场所有 tr
            List<WebElement> listKeChang = new ArrayList<>();
            try {
                // 执行 js 方法，显示客场比赛
                ((JavascriptExecutor) driver).executeScript("showTS('B',1)");
                listKeChang = webElement.findElement(By.id("tbTeamHistory_B_away"))
                        .findElements(By.className("tr_l2"));
                try {
                    // 可能会存在一场比赛，就只有 tr_10 ，tr_l1 会找不到报错
                    listKeChang.addAll(webElement.findElement(By.id("tbTeamHistory_B_away"))
                            .findElements(By.className("tr_l3")));
                } catch (Exception e) {
                    System.out.println("规则2 " + zhuKe + " 客场>只有一场比赛");
                }
            } catch (Exception e) {
                System.out.println("规则2 " + zhuKe + " 客场>未找到比赛记录");
            }

            // 汇总
            listZhuChangAll.addAll(listZhuChang);
            listZhuChangAll.addAll(listKeChangAll);
            listZhuChangAll.addAll(listKeChang);

            Integer listSize = listZhuChangAll.size();

            // 筛选 tr
            listZhuChangAll = filterGuiZe2(listZhuChangAll, zhuKe);

            //Long endTime111 = System.currentTimeMillis();
            //System.out.println("规则2 " + zhuKe + ",汇总 消耗时间：" + (endTime111 - beginTime));

            Long endTime = System.currentTimeMillis();
            System.out.println("规则2 " + zhuKe + ",共" + listSize + "场比赛，抓取数据消耗时间：" + (endTime - beginTime));
            return listZhuChangAll.size();
        } catch (Exception e) {
            System.out.println(zhuKe + " 规则2，抓取数据异常！！！！");
            return -1;
        }
    }

    /**
     * 过滤比赛
     * 1、不是“球会友谊”
     * 2、只要当年比赛
     * 3、只要0-0，或不存在0-0的比赛
     *
     * @param list
     * @return
     */
    private List<WebElement> filterGuiZe2(List<WebElement> list, String zhuKe) {
        // 排除 赛事 “球会友谊”
        final String EXCLUDE_GAME_NAME = "球会友谊";

        List<WebElement> newList = new ArrayList<>();
        // 分数定义在这里，避免循环体内频繁创建对象占用内存，
        WebElement web;
        String score;
        for (int i = 0, size = list.size(); i < size; i++) {
            try {
                web = list.get(i);
                // 找到得分
                score = web.findElement(By.className("td_score")) // 从tr里面找到 分数td
                        .findElement(By.tagName("a")).getAttribute("innerHTML"); // 从分数td 里面找到 a 标签
                //.replace("<span>", "").replace("</span>", ""); // 替换掉没用元素

                //System.out.println("HTML=======规则2" + zhuKe + " 比分HTML:" + score);

                if ("0-0".equals(score)) {
                    //System.out.println("进进进=======规则2" + zhuKe + "0-0");
                    // 排除不需要的tr(1、球会友谊 的比赛不要；2、只要当年的数据；)
                    if (!isGameName(web, EXCLUDE_GAME_NAME, zhuKe) && isNowYear(web, zhuKe)) {
                        newList.add(list.get(i));
                    }
                }
            } catch (Exception e) {
                // 异常了说明，当前 tr 不是一个比赛的 tr
            }
        }
        return newList;
    }


    /**
     * 判断赛事名称是否是 传入的值 excludeGameName
     */
    private boolean isGameName(WebElement web, String excludeGameName, String zhuKe) {
        //Long beginTime = System.currentTimeMillis();
        String gameName = "";
        try {
            // td class = tr_l1 赛事名称td
            // 从tr里面根据class 找到 赛事名称 td ,取出文本值
            /**
             * WebDriver判定isDisplayed为false的元素,那么getText()将为空
             * isDisplayed为false的元素,依然可以通过getAttribute()方法获取元素的属性.
             */
            gameName = web.findElement(By.className("td_lea")).getAttribute("innerHTML").trim();
            //System.out.println("进进进=======规则2" + zhuKe + ",gameName:" + gameName);
            //gameName = web.findElements(By.tagName("td")).get(0).getAttribute("innerHTML").trim();
        } catch (Exception e) {
            System.out.println("判断当前行的赛事名称是否是" + excludeGameName + "异常");
        }
        //Long endTime = System.currentTimeMillis();
        //System.out.println("判断 比赛队伍 消耗时间：" + (endTime - beginTime));
        return excludeGameName.equals(gameName);
    }

    /**
     * 判断，主场、客场比赛时间是否是当年
     */
    private boolean isNowYear(WebElement web, String zhuKe) {
        String time;
        String[] gameTime;
        try {
            // td class = tr_l1 赛事名称td
            // 从tr里面根据class 找到 赛事名称 td ,取出文本值
            /**
             * WebDriver判定isDisplayed为false的元素,那么getText()将为空
             * isDisplayed为false的元素,依然可以通过getAttribute()方法获取元素的属性.
             */
            time = web.findElement(By.className("td_times")).getAttribute("innerHTML").trim();
            //System.out.println("进进进=======规则2" + zhuKe + ",gameTime:" + time);
            gameTime = time.split("-");
        } catch (Exception e) {
            System.out.println("获取当前比赛时间异常");
            e.printStackTrace();
            return false;
        }

        if (gameTime.length == 0) {
            return false;
        }
        // 赛事年份 yy
        String gameYear = gameTime[0];

        // 获取当前年份 yy
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd");
        String nowTime = sdf.format(new Date());
        String[] nowTimeArray = nowTime.split("-");
        String nowYear = nowTimeArray[0];

        return nowYear.equals(gameYear);
    }


    /**
     * 抓取数据并把数据写入到文件
     */
    @Override
    public Integer zhuaDataAndCreateFile(Integer guiZe) {
        if (lock.tryLock()) {
            try {
                // 抓数据
                zhua(guiZe);
                // 创建txt文件
                //createDataFile(guiZe);
                // 创建excel文件
                createExcelFile(guiZe);
            } catch (Exception e) {
                System.out.println("抓取数据并把数据写入到文件异常" + e);
                return 2;
            } finally {
                lock.unlock();
            }
        } else {
            // 获取锁失败
            return 1;
        }
        // 成功
        return 0;
    }

    /**
     * 下载文件
     */
    @Override
    public void download(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /*// 抓数据
        zhua();
        // 创建文件
        createDataFile();*/

        if (pathFileName != null) {
            File file = new File(pathFileName);
            // 如果文件存在，则进行下载
            if (file.exists()) {
                // 配置文件下载
                response.setHeader("content-type", "application/octet-stream");
                //response.setContentType("application/octet-stream");
                // 设置内容类型
                response.setContentType("text/html");
                // 下载文件能正常显示中文
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

                // 实现文件下载
                byte[] buffer = new byte[1024];
                FileInputStream fis = null;
                BufferedInputStream bis = null;
                try {
                    fis = new FileInputStream(file);
                    bis = new BufferedInputStream(fis);
                    OutputStream os = response.getOutputStream();
                    int i = bis.read(buffer);
                    while (i != -1) {
                        os.write(buffer, 0, i);
                        i = bis.read(buffer);
                    }
                    System.out.println("Download the song successfully!");
                    // log.info("Download the song successfully!");
                } catch (Exception e) {
                    System.out.println("Download the song failed!");
                    // log.info("Download the song failed!");
                } finally {
                    if (bis != null) {
                        bis.close();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    // 更新文件名，防止直接调用下载链接，再次下载
                    // file.renameTo();
                }
            }
        }
    }

    /**
     * 创建txt文件
     *
     * @param guiZe
     * @throws IOException
     */
    private void createDataFile(Integer guiZe) throws IOException {
        FileWriter fw = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            fileName = sdf.format(new Date()) + "_规则" + guiZe + ".txt";
            pathFileName = filePath + fileName;

            // 创建文件
            File file = new File(pathFileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            // 写入数据
            fw = new FileWriter(file);
            fw.write(result.toString());
        } catch (Exception e) {
            System.out.println("下载文件错误！");
            // log.info("下载文件错误！");
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    /**
     * 创建Excel文件
     *
     * @param guiZe
     * @throws IOException
     */
    private void createExcelFile(Integer guiZe) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            fileName = sdf.format(new Date()) + "_规则" + guiZe + ".xls";
            pathFileName = filePath + fileName;

            // 存在文件，删除文件
            File file = new File(pathFileName);
            if (file.exists()) {
                file.delete();
            }

            // Excel头
            String[] titleStrArray = {"联赛名称", "比赛时间", "主队", "客队"};
            // Excel内容
            List<List<String>> listContent = new ArrayList<>();
            if (!StringUtils.isEmpty(result)) {
                // 每行数据
                String[] strArray = result.toString().split("\r\n");
                for (int i = 0; i < strArray.length; i++) {
                    List<String> listStr = new ArrayList<>();
                    // 从每行数据里面找到对应比赛信息
                    String[] str = strArray[i].split(">");
                    // 联赛名称
                    String gameName = str[0].trim();
                    // 联赛时间
                    String gameTime = str[1].trim();
                    // 参赛队伍
                    String gameTeam = str[2];
                    String[] team = gameTeam.split("VS");
                    // 主队
                    String zhu = team[0].trim();
                    // 客队
                    String ke = team[1].trim();

                    listStr.add(gameName);
                    listStr.add(gameTime);
                    listStr.add(zhu);
                    listStr.add(ke);
                    listContent.add(listStr);
                }
            }

            // 创建Excel文件
            ExcelUtil.createExcel(listContent, titleStrArray, pathFileName);
        } catch (Exception e) {
            System.out.println("创建Excel文件错误！");
        }
    }

    /**
     * 校验密码
     *
     * @return
     */
    public Integer checkPassword(String password) {
        if (StringUtils.isEmpty(password) || !PASSWORD.equalsIgnoreCase(password)) {
            // 密码失败
            return 1;
        } else {
            return 0;
        }
    }

    private void isTime() {
        if (!isTime) {

        }
    }

    /**
     * 获取比赛数量
     *
     * @return
     */
    public Integer getGameCount() {
        return gameCount;
    }

    /**
     * 获取已抓取的比赛数量
     *
     * @return
     */
    public Integer getAlreadyCount() {
        return count.get();
    }

    public String updatePassword(String password) {
        PASSWORD = password;
        return "update password success ";
    }


    public static void main(String[] args) {
        String resultString = "伊朗甲 > 76' > 帕克达什特 VS 厄尔布尔士,\n" +
                "葡U23 > 78' > 阿维斯(U23) VS 费马利卡奥(U23),\n" +
                "亚美甲 > 84' > 勒那因 VS 阿尼叶里温,\n" +
                "球会友谊 > 83' > 咸美顿后备队 VS 登地联后备队,\n" +
                "伊朗甲 > 89' > 斯兰泽游牧者 VS 拉夫桑贾,\n" +
                "威冠北 > 02:30 > 弗林特镇 VS 康威保罗夫,\n" +
                "球会友谊 > 73' > 阿伯丁后备队 VS 罗斯郡后备队,\n" +
                "伊朗甲 > 64' > 玛拉宛 VS 达玛希吉兰,\n" +
                "伊朗甲 > 62' > 阿拉克铝业 VS 乌尔米耶,\n" +
                "法联杯 > 02:00 > 阿雅克肖GFCO VS 巴黎足球会,\n" +
                "北爱联杯 > 02:45 > 高利宁 VS 阿纳格联,\n" +
                "北爱联杯 > 02:45 > 邓迪拉 VS 托贝摩尔联,\n" +
                "乌克杯 > 23:00 > 沃尔昌斯克 VS 奥布隆,\n" +
                "丹麦U17 > 23:59 > 哥本哈根(U17) VS 法鲁姆(U17),\n" +
                "英锦赛 > 02:45 > 曼斯菲特 VS 埃弗顿(U21),\n" +
                "阿拉冠 > 03:30 > CS康桑汰 VS 穆哈拉格,\n" +
                "球会友谊 > 05:30 > 独立AC(U20) VS 胡玛塔AC(U20),\n" +
                "墨西U20 > 23:00 > 圣路易斯(U20) VS 莫雷利亚(U20),\n" +
                "葡U23 > 23:00 > 布拉加(U23) VS 樸迪莫伦斯(U23),\n" +
                "墨西U20 > 23:59 > 墨西哥美洲队(U20) VS 帕丘卡(U20),\n" +
                "南球杯 > 08:30 > 拉伊奎达德(中) VS 米涅罗竞技,\n" +
                "中北美联 > 10:00 > 珊卡洛斯 VS 圣达迪卡拉,\n" +
                "巴乙 > 06:15 > 科里蒂巴 VS 维多利亚,\n" +
                "德南联 > 01:00 > 洛特维科布伦茨 VS 萨尔布吕肯,\n" +
                "巴乙 > 06:15 > 博塔弗戈SP VS 巴拉纳,\n" +
                "南非超 > 01:30 > 海兰德斯公园 VS 阿马祖路,\n" +
                "巴乙 > 07:30 > 克里西乌马 VS 欧斯特,\n" +
                "巴乙 > 08:30 > 累西腓体育 VS 戈亚尼恩斯竞技,\n" +
                "乌拉后备 > 02:00 > P.利物浦后备队 VS 佩拉扎科朗尼亚后备队,\n" +
                "伊朗甲 > 82' > 库内巴博勒 VS 巴德兰,\n" +
                "墨西U20 > 41' > 阿特拿斯(U20) VS 塔格雷斯(U20),\n" +
                "威冠北 > 02:30 > 宾高城 VS 巴克利镇,\n" +
                "球会友谊 > 02:30 > 科泰尼沙 VS 费罗维里亚CE,\n" +
                "英联杯 > 02:45 > 保顿艾尔宾 VS 莫雷坎比,\n" +
                "北爱联杯 > 02:45 > 罗夫加尔 VS 纽维城,\n" +
                "英联杯 > 02:45 > 罗奇代尔 VS 卡利斯尔联,\n" +
                "英联杯 > 02:45 > 卡迪夫城 VS 卢顿,\n" +
                "英联杯 > 02:45 > 水晶宫 VS 科切斯特联,\n" +
                "沙特甲 > 23:45 > 阿尔吉尔 VS 布凯里耶,\n" +
                "英联杯 > 02:45 > 谢菲尔德联 VS 布莱克本,\n" +
                "巴乙 > 06:15 > 布拉希尔佩洛塔斯 VS 庞特普雷塔,\n" +
                "巴乙 > 07:30 > 欧帕瑞欧PR VS 费古埃伦斯,\n" +
                "哥伦乙 > 04:00 > 拉尼罗斯 VS 利昂内斯,";

        String[] resultArray = resultString.split(",");
        List<Map<String, String>> listMap = new ArrayList<>();
        for (int i = 0; i < resultArray.length; i++) {
            Map<String, String> map = new HashMap<>();
            String time = resultArray[i].split(">")[1].trim();
            map.put(time, resultArray[i]);
            listMap.add(map);
        }


        Collections.sort(listMap, (o1, o2) -> {
            String key1 = o1.keySet().iterator().next();
            String key2 = o2.keySet().iterator().next();
            return key1.compareToIgnoreCase(key2);
        });

        System.out.println("==========================排序后结果==========================");
        for (int i = 0; i < listMap.size(); i++) {
            Map<String, String> map = listMap.get(i);
            for (Map.Entry<String, String> m : map.entrySet()) {
                System.out.println(m.getValue());
            }
        }
    }
}
