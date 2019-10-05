package com.example.zhuashuju.zhua.controller;

import com.example.zhuashuju.zhua.service.Zhua7mService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/zhua")
public class Zhua7mController {
    private static Integer aaa = 0;

    @Autowired
    private Zhua7mService service;

    /*@RequestMapping("/")
    public String Zhua7mData() {
        return service.zhua();
    }*/

    @GetMapping("/zhuaDataAndCreateFile")
    public Integer ZhuaDataAndCreateFile(@RequestParam Integer guiZe) {
        return service.zhuaDataAndCreateFile(guiZe);
    }

    @GetMapping("/download")
    public void Zhua7mDataDownload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.download(request, response);
    }

    @GetMapping("/checkPassword")
    public Integer checkPassword(@RequestParam String password) {
        return service.checkPassword(password);
    }

    @GetMapping("/getGameCount")
    public Integer getGameCount() {
        return service.getGameCount();
    }

    @GetMapping("/getAlreadyCount")
    public Integer getAlreadyCount() {
        return service.getAlreadyCount();
    }

    @GetMapping("/updatePassword")
    public String updatePassword(@RequestParam String password) {
        return service.updatePassword(password);
    }
}
