package com.example.zhuashuju.zhua.excel;

import lombok.Data;

/**
 * Excel 内容对象
 * @author litong create on 2019-09-30
 * excel 需要显示的内容
 */
@Data
public class Member {
    /**
     * 赛事名称
     */
    private String gameName;
    /**
     * 比赛时间
     */
    private String time;
    /**
     * 主队
     */
    private String zhu;
    /**
     * 客队
     */
    private String ke;
}
