package com.example.zhuashuju.zhua.excel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建excel，写入数据，然后下载到本地
 *
 * @author baiyu
 *
 */
public class DownloadExcel {

    public static void main(String[] args) throws IOException {
        String[] strArray = excelTitle();
        List<List<String>> allContent = excelContent();
        ExcelUtil.createExcel(allContent, strArray, "D:/zhua/Members.xls");
    }


    /**
     * 创建excel title
     */
    public static String[] excelTitle() {
        String[] strArray = { "联赛名称", "比赛时间", "主队", "客队" };
        return strArray;
    }

    /**
     * 创建excel 内容
     */
    public static List<List<String>> excelContent() {
        List<List<String>> allContent = new ArrayList<>();
        List<String> content = new ArrayList<>();
        content.add("a");
        content.add("b");
        content.add("c");
        content.add("d");

        allContent.add(content);
        return allContent;
    }
}
