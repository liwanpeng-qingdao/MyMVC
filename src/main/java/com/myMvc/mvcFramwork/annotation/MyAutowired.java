package com.myMvc.mvcFramwork.annotation;

import java.lang.annotation.*;

/**
 * Created by liwanpeng on 2018/1/2.
 */
@Target(ElementType.FIELD)//注解的作用目标TYPE-类，FIELD-字段，METHOD-方法，PARAMETER-参数，
@Retention(RetentionPolicy.RUNTIME)
@Documented//这个注解应该被 javadoc工具记录. 默认情况下,javadoc是不包括注解的.
public @interface MyAutowired {
    String value() default "";
}
