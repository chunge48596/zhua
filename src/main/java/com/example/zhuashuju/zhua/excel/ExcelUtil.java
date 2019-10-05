package com.example.zhuashuju.zhua.excel;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelUtil {
    /**
     * @功能：手工构建一个简单格式的Excel
     * @param members 数据体
     * @param titleArray Excel 头
     */
    public static void createExcel(List<List<String>> members, String[] titleArray, String filePath) throws IOException {
        // 第一步，创建一个webbook，对应一个Excel文件
        HSSFWorkbook wb = new HSSFWorkbook();
        // 第二步，在webbook中添加一个sheet,对应Excel文件中的sheet
        HSSFSheet sheet = wb.createSheet("sheet1");
        sheet.setDefaultColumnWidth(20);// 默认列宽
        // 第三步，在sheet中添加表头第0行,注意老版本poi对Excel的行数列数有限制short
        HSSFRow row = sheet.createRow(0);

        // 第四步，创建标题栏单元格样式
        HSSFCellStyle titleStyle = titleStyle(wb);

        // 添加Excel title
        HSSFCell titleCell;
        for (int i = 0; i < titleArray.length; i++) {
            titleCell = row.createCell((short) i);
            titleCell.setCellValue(titleArray[i]);
            // 标题栏样式
            titleCell.setCellStyle(titleStyle);
        }

        // 创建内容单元格样式
        HSSFCellStyle contentStyle = contentStyle(wb);

        // 第五步，写入实体数据 实际应用中这些数据从数据库得到,list中字符串的顺序必须和数组strArray中的顺序一致
        for (int j = 0,size = members.size(); j < size; j++) {
            // 创建行，这里加1，因为有title
            row = sheet.createRow(j + 1);
            // 第j行
            List<String> line = members.get(j);
            // 第四步，创建单元格，并设置值
            HSSFCell contentCell;
            for (int unit = 0; unit < line.size(); unit++) {
                contentCell = row.createCell((short) unit);
                contentCell.setCellValue(line.get(unit));
                // 设置内容样式
                contentCell.setCellStyle(contentStyle);
            }

            // 第六步，将文件存到指定位置
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(filePath);
                wb.write(fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                fos.close();
            }
        }
    }

    /**
     * 标题样式
     * @param wb
     * @return
     */
    private static HSSFCellStyle titleStyle(HSSFWorkbook wb) {
        // 创建单元格样式，设置值表头 设置表头居中
        HSSFCellStyle style = wb.createCellStyle();
        style = commonStyle(style);

        // 设置字体
        HSSFFont font = wb.createFont();
        font.setFontName("黑体");
        font.setFontHeightInPoints((short) 14);//设置字体大小
        font.setBold(true);//粗体显示
        style.setFont(font);

        // 背景颜色，没有效果
        //style.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.LIGHT_TURQUOISE.getIndex());
        return style;
    }

    /**
     * 内容样式
     * @param wb
     * @return
     */
    private static HSSFCellStyle contentStyle(HSSFWorkbook wb) {
        // 创建单元格样式，设置值表头 设置表头居中
        HSSFCellStyle style = wb.createCellStyle();
        style = commonStyle(style);

        // 设置字体
        HSSFFont font = wb.createFont();
        font.setFontName("黑体");
        font.setFontHeightInPoints((short) 12);//设置字体大小
        font.setBold(false);//粗体显示
        style.setFont(font);
        return style;
    }

    /**
     * 公共样式
     * @param style
     * @return
     */
    private static HSSFCellStyle commonStyle(HSSFCellStyle style) {
        // 创建单元格样式，设置值表头 设置表头居中
        style.setAlignment(HorizontalAlignment.CENTER);// 水平居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);//垂直居中

        style.setBorderBottom(BorderStyle.THIN); //下边框
        style.setBorderLeft(BorderStyle.THIN);//左边框
        style.setBorderTop(BorderStyle.THIN);//上边框
        style.setBorderRight(BorderStyle.THIN);//右边框
        return style;
    }
}
