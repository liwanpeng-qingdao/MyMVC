package com.myMvc.mvcFramwork.annotation;

import com.sun.org.apache.regexp.internal.RE;

import java.lang.annotation.*;

/**
 * Created by liwanpeng on 2018/1/2.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    String value() default "";
}
