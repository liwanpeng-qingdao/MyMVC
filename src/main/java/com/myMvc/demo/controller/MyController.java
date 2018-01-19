package com.myMvc.demo.controller;


import com.myMvc.demo.service.ModifyService;
import com.myMvc.demo.service.QueryService;
import com.myMvc.mvcFramwork.annotation.MyAutowired;
import com.myMvc.mvcFramwork.annotation.MyRequestMapping;
import com.myMvc.mvcFramwork.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by liwanpeng on 2018/1/3.
 */
@MyRequestMapping("/myController")
@com.myMvc.mvcFramwork.annotation.MyController
public class MyController {
    @MyAutowired("myQueryService")
    private QueryService queryService;
    @MyAutowired
    private ModifyService modifyService;

    @MyRequestMapping("/search")
    public void search(@MyRequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
        String result = queryService.search(name);
        out(response, result);
    }

    @MyRequestMapping("/add")
    public void add(@MyRequestParam("name") String name, @MyRequestParam("addr") String addr,
                    HttpServletRequest request, HttpServletResponse response) {
        String result = modifyService.add(name, addr);
        out(response, result);
    }

    @MyRequestMapping("/remove")
    public void remove(@MyRequestParam("name") Integer id,
                       HttpServletRequest request, HttpServletResponse response) {
        String result = modifyService.remove(id);
        out(response, result);
    }

    private void out(HttpServletResponse response, String str) {
        try {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
