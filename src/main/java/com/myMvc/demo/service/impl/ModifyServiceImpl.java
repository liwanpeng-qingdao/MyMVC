package com.myMvc.demo.service.impl;

import com.myMvc.demo.service.ModifyService;
import com.myMvc.mvcFramwork.annotation.MyService;

/**
 * Created by liwanpeng on 2018/1/3.
 */
@MyService
public class ModifyServiceImpl implements ModifyService {
    @Override
    public String add(String name, String addr) {
        return "invoke add name = " + name + " addr = " + addr;
    }

    @Override
    public String remove(Integer id) {
        return "remove id = " + id;
    }
}
