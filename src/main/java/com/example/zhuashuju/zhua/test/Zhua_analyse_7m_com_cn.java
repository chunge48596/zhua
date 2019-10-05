package com.example.zhuashuju.zhua.test;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * http://chromedriver.storage.googleapis.com/index.html 这个url坑死了，里面对应的版本都不对
 * http://npm.taobao.org/mirrors/chromedriver/ 找到对应版本的 dirver
 * ChromeDriver 仓库
 * 自己电脑装的 chrome 版本是 V76
 */
public class Zhua_analyse_7m_com_cn {

    private static WebDriver driver;

    public static void main(String[] args) {
        /*System.getProperties().setProperty("webdriver.chrome.driver",
                "D:\\soft\\chromedriver_win32\\chromedriver.exe");
        WebDriver webDriver = new ChromeDriver();
        webDriver.get("http://bf.7m.com.cn/default_split_gb.aspx?view=all&match=&line=no");
        WebElement webElement = webDriver.findElement(By.xpath("/html"));*/

        // todo 获得当前行的HTML .getAttribute("outerHTML")
        //String leftTableHtml = leftTabel1.findElements(By.tagName("tr")).get(1).getAttribute("outerHTML");

        // 编号 找到所有tr元素，取第二条tr，获取tr里面 name = "hiddenChk" 的元素，然后取出该元素的value属性值
        //String bianhao = leftTabel1.findElements(By.tagName("tr")).get(1).findElement(By.name("hiddenChk")).getAttribute("value");

        // 主队 找到所有tr元素，取第二条tr，获取tr里面 所有 <a> 标签元素，取出第一个 <a> 标签的 text 文本
        //String zhu = leftTabel1.findElements(By.tagName("tr")).get(1).findElements(By.tagName("a")).get(0).getText();

        // 客队
        // String ked = leftTabel1.findElements(By.tagName("tr")).get(1).findElements(By.tagName("a")).get(2).getText();

        // 找出 完 的元素
        // String complate = leftTabel1.findElements(By.tagName("tr")).get(1).findElement(By.className("state")).getText();

        driver = getWebDriver("http://bf.7m.com.cn/default_split_gb.aspx?view=all&match=&line=no");
        driver.get("http://bf.7m.com.cn/default_split_gb.aspx?view=all&match=&line=no");
        WebElement webElement = driver.findElement(By.xpath("/html"));

        /**
         * 找到两个比赛table列表
         * <table class="live" cellpadding="2" cellspacing="1" id="left_live_Table">
         * <table class="live" cellpadding="2" cellspacing="1" id="right_live_Table">
         */
        List<WebElement> liveTables = webElement.findElements(By.className("live"));
        for (WebElement tableWe : liveTables) {
            // 找到table中的所有tr元素
            List<WebElement> trs = tableWe.findElements(By.tagName("tr"));
            trs = trs.subList(0,5);
            for (WebElement trWe : trs) {
                // 先判断是否是比赛的tr <input type="checkbox" name="hiddenChk" id="hiddenChk3946398" value="3946398" onclick="javascript:showselMdiv(this)">
                // 取不到元素，直接抛出异常了
                WebElement input;
                try {
                    input = trWe.findElement(By.name("hiddenChk"));
                } catch (Exception e) {
                    continue;
                }

                if (input != null) { // 有比赛
                    // 找到tr行中 完 用于判断是否需要这个比赛
                    String complete = trWe.findElement(By.className("state")).getText();
                    if (complete.equalsIgnoreCase("完")) {
                        continue;
                    }

                    // 编号
                    String bianhao = input.getAttribute("value");

                    String sai = trWe.findElements(By.className("match")).get(0).getText();


                    // 找到tr行中主队
                    String zhuDui = trWe.findElements(By.tagName("a")).get(0).getText();

                    // 找到tr行中客队
                    String keDui = trWe.findElements(By.tagName("a")).get(2).getText();

                    System.out.println("===========================比赛队伍：" + zhuDui + " VS " + keDui + "编号：" + bianhao);

                    /**
                     * 1、主场总场数，有没有0:0
                     * 2、主场，有没有0:0
                     * 3、客场总场数，有没有0:0
                     * 4、客场，有没有0:0
                     */
                    if (isZoneZone(bianhao, zhuDui, keDui)) {
                        System.out.println("" + zhuDui + " VS " + keDui);
                    }
                }
            }
        }

        driver.quit();
    }

    /**
     * 获取web驱动
     *
     * @param url
     * @return
     */
    public static WebDriver getWebDriver(String url) {
        try {
            //设置chrome选项 不弹出网页
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");

            System.getProperties().setProperty("webdriver.chrome.driver",
                    "D:\\soft\\chromedriver_win32\\chromedriver.exe");
            if (driver == null) {
                driver = new ChromeDriver(options);
            }
            //driver.get(url);
        } catch (Throwable e){

        }
        return driver;
    }

    /**
     * 1、主场总场数，有没有0:0
     * 2、主场，有没有0:0
     * 3、客场总场数，有没有0:0
     * 4、客场，有没有0:0
     *
     * @param bianhao
     * @return
     */
    public static boolean isZoneZone(String bianhao, String zhuDui, String keDui) {
        // 析 URL
        StringBuffer url = new StringBuffer("http://analyse.7m.com.cn/");
        url.append(bianhao).append("/index_gb.shtml");
        driver = getWebDriver(url.toString());
        driver.get(url.toString());
        // 把所有table 都显示，不让隐藏 不好使
        // String js1 = "document.getElementById(\"divTeamHistoryA1\").style.display=\"block\";";
        //((JavascriptExecutor) driver).executeScript("showTS('A',1)");

        WebElement webElement = driver.findElement(By.xpath("/html"));
        String zhuKe = zhuDui + " VS " + keDui;
        // 主场all eg:0:0
        String homeAllScore = "";
        try {
            homeAllScore = webElement.findElement(By.id("tbTeamHistory_A_all")) // 找到主场所有场次table
                    .findElements(By.tagName("tr")).get(2) // 找到 第三行，首场比赛 tr
                    .findElement(By.className("td_score")) // 找到 td
                    .findElement(By.tagName("a")).getText() // 获取 a 标签的文本
                    .replace("<span>", "").replace("</span>", ""); // 替换掉没用元素
            System.out.println(zhuKe + " 主场所有场次>: " + homeAllScore);

        } catch (Exception e) {
            System.out.println(zhuKe + " 主场所有场次>未找到比赛记录");
            //e.printStackTrace();
        }

        // 主场home
        String homeScore = "";
        try {
            // 执行 js 方法，显示主场比赛
            ((JavascriptExecutor) driver).executeScript("showTS('A',1)");
            homeScore = webElement.findElement(By.id("tbTeamHistory_A_home")) // 找到主场所有场次table
                    .findElements(By.tagName("tr")).get(2) // 找到 第三行，首场比赛 tr
                    .findElement(By.className("td_score")) // 找到 td
                    .findElement(By.tagName("a")).getText() // 获取 a 标签的文本
                    .replace("<span>", "").replace("</span>", ""); // 替换掉没用元素
            System.out.println(zhuKe + " 主场>: " + homeScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 主场>未找到比赛记录");
            //e.printStackTrace();
        }

        // 客场all
        String awayAllScore = "";
        try {
            awayAllScore = webElement.findElement(By.id("tbTeamHistory_B_all")) // 找到主场所有场次table
                    .findElements(By.tagName("tr")).get(2) // 找到 第三行，首场比赛 tr
                    .findElement(By.className("td_score")) // 找到 td
                    .findElement(By.tagName("a")).getText() // 获取 a 标签的文本
                    .replace("<span>", "").replace("</span>", ""); // 替换掉没用元素
            System.out.println(zhuKe + " 客场总场次>: " + awayAllScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 客场总场次>未找到比赛记录");
            //e.printStackTrace();
        }

        // 客场away
        String awayScore = "";
        try {
            // 执行 js 方法，显示客场比赛
            ((JavascriptExecutor) driver).executeScript("showTS('B',1)");
            awayScore = webElement.findElement(By.id("tbTeamHistory_B_away")) // 找到主场所有场次table
                    .findElements(By.tagName("tr")).get(2) // 找到 第三行，首场比赛 tr
                    .findElement(By.className("td_score")) // 找到 td
                    .findElement(By.tagName("a")).getText() // 获取 a 标签的文本
                    .replace("<span>", "").replace("</span>", ""); // 替换掉没用元素
            System.out.println(zhuKe + " 客场>: " + awayScore);
        } catch (Exception e) {
            System.out.println(zhuKe + " 客场>未找到比赛记录");
            //e.printStackTrace();
        }

        List<String> scores = new ArrayList<>();
        scores.add(homeAllScore);
        scores.add(homeScore);
        scores.add(awayAllScore);
        scores.add(awayScore);
        if (scores.contains("0-0")) {
            return true;
        }
        //driver.close();
        return false;
    }
}
