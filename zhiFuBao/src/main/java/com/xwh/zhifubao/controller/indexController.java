package com.xwh.zhifubao.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class indexController {

    @GetMapping({"/", "index.html", "index"})
    public String index() {
        return "index";
    }

//    public String
}
