package com.example.zhuashuju.zhua.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Zhua7mService {

    /**
     * 抓数据
     *
     * @return
     */
    String zhua(Integer guiZe);

    /**
     * 抓取数据并把数据写入到文件
     */
    Integer zhuaDataAndCreateFile(Integer guiZe);

    /**
     * 先抓数据，再下载数据
     *
     * @param request
     * @param response
     * @throws IOException
     */
    void download(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * 获取比赛数量
     *
     * @return
     */
    Integer checkPassword(String password);

    /**
     * 获取比赛数量
     *
     * @return
     */
    Integer getGameCount();

    /**
     * 获取已抓取的比赛数量
     *
     * @return
     */
    Integer getAlreadyCount();

    String updatePassword(String password);
}
