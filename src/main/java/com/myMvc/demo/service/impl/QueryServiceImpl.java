package com.myMvc.demo.service.impl;

import com.myMvc.demo.service.QueryService;
import com.myMvc.mvcFramwork.annotation.MyService;

/**
 * Created by liwanpeng on 2018/1/3.
 */
@MyService
public class QueryServiceImpl implements QueryService {
    @Override
    public String search(String name) {
        return "invoke search name = " + name;
    }
}
