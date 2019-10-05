package com.example.zhuashuju.zhua.test;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;

/**
 * 抓数据，
 *
 * @author litong 2019-08-17
 */
public class Zhua_500 {

    public static void main(String[] argc) throws InterruptedException {
        String html = requestURL("https://trade.500.com/jczq/index.php?playid=271&g=2", "gb2312", null);
        doGetData(html);

        //fightHistory("c:\\jiaozhan.html");
    }

    public static void doGetData(String html) throws InterruptedException {
        if (html != null) {
            // 网站只显示最近三天的比赛，这个标记是比赛日期的样式
            // <span class="bet-date" data-num="44">2019-08-18 星期天</span>
            String[] tr = html.split("<span class=\"bet-date\" data-num=\"");
            // 数组下标从 1 开始
            for (int i = 1; i < tr.length; i++) {
                // 获得有几场比赛
                int index = tr[i].indexOf("\">");
                int gameCount = Integer.parseInt(tr[i].substring(0, index));
                // 获取时间
                int index1 = tr[i].indexOf(" ");
                String time = tr[i].substring(index + 2, index1);
                // 计算周几
                //<a href="javascript:;" class="bet-evt-hide" title="点击可隐藏该比赛">周六001<s></s></a>
                String week = dateToWeek(time, 2);

                System.out.println(time + " " + week + ",共 " + gameCount + " 场比赛 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

                // 获取首场比赛的下标和比赛编号
                String gameStarIndexAndNum = getGameIndexAndStartNum(tr[i], week, 1);
                //Integer firstIndex = Integer.parseInt(gameStarIndexAndNum.split(",")[0]);
                Integer firstNum = Integer.parseInt(gameStarIndexAndNum.split(",")[1]);
                Integer endNum = gameCount;

                // 获取每一场比赛的HTML内容
                for (int j = firstNum; j <= endNum; j++) {
                    Thread.sleep(1000);
                    String zhou = week + String.format("%03d", j);
                    String oneGameHTML;
                    if (j != endNum) {
                        // eg：周六001
                        Integer firstIndex = tr[i].indexOf(zhou);
                        // eg：周六002
                        Integer endIndex = tr[i].indexOf(week + String.format("%03d", j + 1));
                        oneGameHTML = tr[i].substring(firstIndex, endIndex);
                    } else {
                        // 最后一场比赛获取方式
                        oneGameHTML = tr[i].split(zhou)[2];
                    }

                    // 联赛
                    Integer lianSaiStart = oneGameHTML.indexOf("联赛\">");
                    // +4 是因为截取的字符串，带‘联赛">’ == 4个字符，再往后2个，就是要找的联赛
                    String lianSai = oneGameHTML.substring(lianSaiStart + 4, lianSaiStart + 6);

                    // 投注截止比赛
                    Integer endTimeStart = oneGameHTML.indexOf("<td class=\"td td-endtime\" title=\"");
                    String endTimeHtml = oneGameHTML.substring(endTimeStart).replace("<td class=\"td td-endtime\" title=\"", "");
                    Integer endTimeEnd = endTimeHtml.indexOf("\">");
                    String endTime = endTimeHtml.substring(0, endTimeEnd);

                    // todo 输出文本
                    System.out.print(zhou + "，" + lianSai + "，" + endTime + "，");


                    // 拿到“析”请求连接
                    //<td class="td td-data"><a href="http://odds.500.com/fenxi/shuju-779050.shtml" target="_blank">析</a>
                    String xiHTMLStart = "<td class=\"td td-data\"><a href=\"";
                    Integer xiStart = oneGameHTML.indexOf(xiHTMLStart);
                    Integer xiEnd = oneGameHTML.indexOf("\" target=\"_blank\">析");
                    String xiUrl = oneGameHTML.substring(xiStart, xiEnd).replace(xiHTMLStart, "");

                    // 请求“析”链接
                    String xiHTMLStr = requestURL(xiUrl, "gb2312", null);

                    // 获取最近交战史
                    doFightHistory(zhou, j, xiHTMLStr);
                }
            }
        } else {
            System.out.println("暂无赛事信息");
        }
    }

    /**
     * 处理“交战历史”
     *
     * @param xiHtmlStr “析” htmlStr
     */
    public static void doFightHistory(String zhou, int xuhao, String xiHtmlStr) {
        try {
            int startIndex = xiHtmlStr.indexOf("两队交战史");
            int endIndex = xiHtmlStr.indexOf("<!-- 近期战绩 -->");
            if (endIndex == -1) {
                endIndex = xiHtmlStr.indexOf("期战绩 -->");
            }
            String jiaoZhan = null;
            try {
                jiaoZhan = xiHtmlStr.substring(startIndex, endIndex);
            } catch (Exception e) {
                System.out.println(xiHtmlStr);
                e.printStackTrace();
            }

            if (jiaoZhan.indexOf("双方暂无交战历史") != -1) {
                //System.out.println("###################" + jiaoZhan);
                int noHistoryStart = jiaoZhan.indexOf("<h4>");
                int noHistoryEnd = jiaoZhan.indexOf("</h4>");
                String noHistoryTeamName = jiaoZhan.substring(noHistoryStart, noHistoryEnd)
                        .replace("交战历史", "").replace("<h4>", "");
                //System.out.println("序号：" + xuhao + "，" + noHistoryTeamName + ",双方暂无交战历史");
                System.out.println(String.format("双方暂无交战历史，比赛队伍：%s", noHistoryTeamName));
                return;
            } else {
                // 找出交战列表
                int index = jiaoZhan.indexOf("<div class=\"M_content\">");
                int index1 = jiaoZhan.indexOf(" </table>");
                //System.out.println("==================" + jiaoZhan);
                String jiaoZhanList = jiaoZhan.substring(index, index1);

                // 只要tr行信息
                // 只要有比分的那行
                String[] jzTr = jiaoZhanList.split("<tr");
                // 有比分那行,第4行
                String biFen = jzTr[3];

                // 比分行，由<td>组成
                String[] biFenTd = biFen.split("<td");

                // 比赛日期，第3行<td>
                String gameTime = biFenTd[2].replace(">", "").replace("</td", "");

                // 比分，第4行<td>
                String score = biFenTd[3];
                int index2 = score.indexOf("<em>");
                int index3 = score.indexOf("</em>");
                // 比分
                String scoreFen = score.substring(index2, index3);

                if (scoreFen.contains("ping")) {
                    scoreFen = scoreFen.replace("<em>", "")
                            .replace("<span class=\"ping\">", "")
                            .replace("</span>", "");
                } else if (scoreFen.contains("ying")) {
                    scoreFen = scoreFen.replace("<em>", "")
                            .replace("<span class=\"ying\">", "")
                            .replace("</span>", "");
                } else {
                    scoreFen = scoreFen.replace("<em>", "")
                            .replace("<span class=\"shu\">", "")
                            .replace("</span>", "");
                }


                //<em><span class="ping">0</span>:<span class="ping">0</span>

                String[] name = score.split("title=\"");
                String biSaiName = name[1];
                int index4 = biSaiName.indexOf("数据分析");
                // 比赛队伍
                String fullName = biSaiName.substring(0, index4);

                //System.out.println(zhou + ",交战历史首场比赛时间：" + gameTime + "，比赛队伍：" + fullName + "，比赛比分：" + scoreFen);

                System.out.println(String.format("历史首场比赛时间：%s，比分：%s，比赛队伍：%s", gameTime, scoreFen, fullName));

            }
        } catch (Exception e) {
            //System.out.println("=======================" + xiHtmlStr);
            e.printStackTrace();
        }
    }


    /**
     * todo 交战历史 慎用，会有问题
     * 解析HTML时注意，各种网站有很多问题存在，如元素没有结束节点，等
     * 这里最好还是别用DOM解析拿元素，还是直接用截取字符串方式
     *
     * @param xiHtmlPath “析”html路径
     * @see this#doFightHistory(String, int, String)
     */
    public static void fightHistory(String xiHtmlPath) {
        //创建一个解析器
        DOMParser parser = new DOMParser();
        //解析HTML文件
        try {
            // 解析HTML时注意，各种网站有很多问题存在，如元素没有结束节点，等
            // 这里最好还是别用DOM解析拿元素，直接截取字符串
            parser.parse(xiHtmlPath);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //myparser.parse("html/test1.html");
        //获取解析后的DOM树
        Document document = parser.getDocument();

        //通过getElementsByTagName获取Node
        NodeList nodeList = document.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            System.out.print(e.getAttribute("href") + "\t");
            System.out.println(e.getTextContent());
        }
    }

    /**
     * 请求URL,并获取网站数据
     *
     */
    /**
     * @param strUrl   请求链接
     * @param charset  编码集，F12，在head里面可以看到网站用的哪种编码集
     * @param htmlName 生成html的完整路径，不需要则传null
     * @return
     */
    public static String requestURL(String strUrl, String charset, String htmlName) {
        URL url;
        FileOutputStream fos;
        InputStream is = null;
        try {
            for (int i = 0; i < 1; i++) {
                url = new URL(strUrl);
                byte bytes[] = new byte[1024 * 10000];
                int index = 0;
                is = url.openStream();
                int count = is.read(bytes, index, 1024 * 100);
                while (count != -1) {
                    index += count;
                    count = is.read(bytes, index, 1);
                }
                ByteArrayInputStream biArrayInputStream = new ByteArrayInputStream(bytes);

                String htmlStr = uncompress(biArrayInputStream, charset);

                // 本地生HTML
                if (htmlName != null) {
                    fos = new FileOutputStream(htmlName);
                    fos.write(htmlStr.getBytes());
                    fos.close();
                }

                return htmlStr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 解压
     *
     * @param in      输入流
     * @param charset 编码集
     * @return
     */
    public static String uncompress(ByteArrayInputStream in, String charset) {
        try {
            // 如果尝试各种编码集都是乱码，可能是网站用 gzip 把网页压缩了
            GZIPInputStream gInputStream = new GZIPInputStream(in);
            byte[] by = new byte[1024];
            StringBuffer strBuffer = new StringBuffer();
            int len;
            while ((len = gInputStream.read(by)) != -1) {
                strBuffer.append(new String(by, 0, len, charset));
            }
            return strBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取首场比赛下标，和比赛编号
     *
     * @param gameHTML
     * @param week
     * @param gameNum  第几场比赛
     * @return 下标, 比赛场次编号
     */
    public static String getGameIndexAndStartNum(String gameHTML, String week, int gameNum) {
        // 这里可能会少了很多场比赛，并不是从001 开始
        // 循环处理,直到index != -1
        for (; ; ) {
            gameNum = gameNum++;
            // 三位数，不足前面补0
            String formatGameNum = String.format("%03d", gameNum);
            int index = gameHTML.indexOf(week + formatGameNum);
            if (index != -1) {
                return index + "," + formatGameNum;
            }
        }
    }

    /**
     * 根据日期获取当天是星期几或周几
     *
     * @param datetime 日期
     * @param type     1：星期几，2：周几
     * @return 星期几
     */
    public static String dateToWeek(String datetime, int type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        String[] weekDays1 = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Calendar cal = Calendar.getInstance();
        Date date;
        try {
            date = sdf.parse(datetime);
            cal.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (type == 1) {
            return weekDays[w];
        }
        return weekDays1[w];
    }
}