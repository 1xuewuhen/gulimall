package com.xwh.gulimall.search1.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @GetMapping("/list.html")
    public String listPage(){

        return "list";
    }
}
