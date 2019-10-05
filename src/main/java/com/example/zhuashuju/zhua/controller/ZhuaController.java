package com.example.zhuashuju.zhua.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/7m")
public class ZhuaController {
    /**
     * 跳转jsp
     * @return
     */
    @RequestMapping("/")
    public ModelAndView Zhua7mData() {
        ModelAndView modelAndView = new ModelAndView("zhua");//设置对应JSP的模板文件
        modelAndView.addObject("name", "ahadfadsfasdfasdf");
        return modelAndView;
    }
}
