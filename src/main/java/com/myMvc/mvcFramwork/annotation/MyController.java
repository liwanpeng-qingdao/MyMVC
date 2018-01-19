package com.myMvc.mvcFramwork.annotation;

import java.lang.annotation.*;

/**
 * Created by liwanpeng on 2018/1/2.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {
    String value() default "";
}
