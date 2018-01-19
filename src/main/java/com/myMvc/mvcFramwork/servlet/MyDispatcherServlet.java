package com.myMvc.mvcFramwork.servlet;

import com.myMvc.mvcFramwork.annotation.*;
import com.myMvc.mvcFramwork.util.Play;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by liwanpeng on 2018/1/2.
 */
public class MyDispatcherServlet extends HttpServlet{
    //存放所有的类名
    private List<String> classNames = new ArrayList<String>();
    //类似spring的容器
    private Map<String,Object> instanceMapping = new HashMap<String, Object>();
    //处理器适配器
    private Map<String, HandlerModel> handlerMapping = new HashMap<String, HandlerModel>();
    //为了调用父类的构造方法
    public MyDispatcherServlet(){super();}

    @Override
    public void  init(ServletConfig servletConfig){
        //1.读取配置文件
        String scanPackage= servletConfig.getInitParameter("sancanPackage");
        //2.扫描指定包下的类
        scanClass(scanPackage);
        instance();
        autowired();
        handlerMapping();

    }


    //拿到所有的类名
    private void scanClass(String packageName){
        //拿到包路径，转换为文件路径
        //packageName是com.myMvc.demo,以.间隔，而文件路径是以/间隔
        URL url = this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.","/"));
        System.out.println("url==========="+url);
        File dir = new File(url.getFile());
        //递归查找所有文件
        for (File file:dir.listFiles())
        {
            //如果是目录就继续递归
            if (file.isDirectory()){
                scanClass(packageName+"."+file.getName());
            }else
            {
                //不是.class文件就不做处理
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                //遍历出来的文件是jvm编译后的.class文件，在target文件夹下
                String className = packageName+"."+file.getName().replace(".class","");
                //将所有类名放入一个list中，准备实例化
                classNames.add(className);
            }
        }
    }
    //实例化所有类名
    private void instance(){
        //利用反射机制将扫描到的类名全部实例化
        if(classNames.size() == 0){ return; }
        try{
            for (String className : classNames) {

                Class<?> clazz = Class.forName(className);
                //没有@Controller、@Service注解标识的类不需要实例化
                if(clazz.isAnnotationPresent(MyController.class)){
                    //getSimpleName() 除去包名,获取类名的简称  例如: MyAction
                    String beanName = lowerFirstChar(clazz.getSimpleName());
                    instanceMapping.put(beanName, clazz.newInstance());
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    //service需要根据value值来实例化
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    //如果value不为空则使用用户自定义的beanName
                    if(!"".equals(beanName.trim())){
                        //beanName 这里就是aa
                        instanceMapping.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //如果自己没有起名字,后面会通过接口自动注入
                    //获得所有接口的实现类，返回值是个数组
                    Class<?> [] interfaces = clazz.getInterfaces();
                    for (Class c : interfaces) {
                        //举例 modifyService->new ModifyServiceImpl（）
                        instanceMapping.put(lowerFirstChar(c.getSimpleName()), clazz.newInstance());
                        break;
                    }
                }else{
                    continue;
                }
            }
        }catch(Exception e){
            e.getStackTrace();
        }
    }
    //实例化完毕，准备注入
    private void autowired(){
        //如果实例化完，容器中没有任何实例，直接返回
        if (instanceMapping.isEmpty())
        {
            return;
        }
        //
        for (Map.Entry<String,Object> entry:instanceMapping.entrySet())
        {
            //获取一个实例的所有属性
            Field[] fields =  entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //如果这个属性没有被MyAutowired注解则跳过
                if(!field.isAnnotationPresent(MyAutowired.class)){ continue; }

                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                //如果是私有属性，设置可以访问的权限
                field.setAccessible(true);
                //自己取的名字   获取注解的值
                String beanName = autowired.value().trim();
                System.out.println("beanName=="+beanName);
                //如果没有自己取名字
                if("".equals(beanName)){
                    //getType()获取该字段声明时的     类型对象   根据类型注入
                    beanName = field.getType().getName();
                }
                try {
                    System.out.println("field.getName()***"+field.getName());
                    // 注入接口的实现类,
                    System.out.println("entry.getValue()======"+entry.getValue());
                    System.out.println("instanceMapping.get(beanName)---------"+instanceMapping.get(beanName));
                    //将Action 这个 类的 IModifyService 字段设置成为   aa 代表的实现类  ModifyServiceImpl
                    field.set(entry.getValue(),instanceMapping.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * 建立url到方法的映射
     */
    private void handlerMapping() {
        if (instanceMapping.isEmpty()) {
            return;
        }
        //遍历托管的对象，寻找Controller
        for (Map.Entry<String, Object> entry : instanceMapping.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            //只处理Controller的，只有Controller有RequestMapping
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }

            //定义url
            String url = "/";
            //取到Controller上的RequestMapping值
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                url += requestMapping.value();
            }

            //获取方法上的RequestMapping
            Method[] methods = clazz.getMethods();
            //只处理带RequestMapping的方法
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping methodMapping = method.getAnnotation(MyRequestMapping.class);
                //requestMapping.value()即是在requestMapping上注解的请求地址，不管用户写不写"/"，我们都给他补上
                String realUrl = url + "/" + methodMapping.value();
                //替换掉多余的"/",因为有的用户在RequestMapping上写"/xxx/xx",有的不写，所以我们处理掉多余的"/"
                realUrl = realUrl.replaceAll("/+", "/");
//                handlerMapping.put(realUrl, method);
                Map<String, Integer> paramMap = new HashMap<String,Integer>();
                //获取所有的参数名
                //获取参数名
                System.out.println("=====>获取参数名");
                String[] paramNames = new String[0];
                try {
                    paramNames = Play.getMethodParameterNamesByAsm4(clazz, method);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //获取参数上的所有注解，一个参数可以有多个注解
                Annotation[][] annotations = method.getParameterAnnotations();
                //获取所有参数的类型，提取Request和Response的索引
                Class<?>[] paramTypes = method.getParameterTypes();
                //遍历
                for (int i=0;i<annotations.length;i++){
                    //获取每个参数上的所有注解
                    Annotation[] anns = annotations[i];
                    if (anns.length == 0) {
                        //如果没有注解，则是如String abc，Request request这种，没写注解的
                        //如果没被RequestParam注解
                        // 如果是Request或者Response，就直接用类名作key；如果是普通属性，就用属性名
                        Class<?> type = paramTypes[i];
                        if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                            paramMap.put(type.getName(), i);
                        } else {
                            //参数没写@RequestParam注解，只写了String name，那么通过java是无法获取到name这个属性名的
                            //通过上面asm获取的paramNames来映射
                            paramMap.put(paramNames[i], i);
                        }
                        continue;
                    }
                    //有注解，就遍历每个参数上的所有注解
                    for (Annotation ans : anns) {
                        //找到被RequestParam注解的参数，并取value值
                        if (ans.annotationType() == MyRequestParam.class) {
                            //也就是@RequestParam("name")上的"name"
                            String paramName = ((MyRequestParam) ans).value();
                            //如果@RequestParam("name")这里面
                            if (!"".equals(paramName.trim())) {
                                paramMap.put(paramName, i);
                            }
                        }
                    }
                    HandlerModel model = new HandlerModel(method, entry.getValue(), paramMap);
                    handlerMapping.put(realUrl, model);
                }

            }

        }

    }
    private class HandlerModel {
        Method method;
        Object controller;
        Map<String, Integer> paramMap;

        public HandlerModel(Method method, Object controller, Map<String, Integer> paramMap) {
            this.method = method;
            this.controller = controller;
            this.paramMap = paramMap;
        }
    }
    //首字母小写
    private String lowerFirstChar(String className) {
        char[] chars = className.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //根据请求的URL去查找对应的method
        try {
            boolean isMatcher = pattern(req, resp);
            if (!isMatcher) {
                out(resp,"404 not found");
            }
        } catch (Exception ex) {
            ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            ex.printStackTrace(new java.io.PrintWriter(buf, true));
            String expMessage = buf.toString();
            buf.close();
            out(resp, "500 Exception" + "\n" + expMessage);
        }
    }
    private boolean pattern(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (handlerMapping.isEmpty()) {
            return false;
        }
        //用户请求地址
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        //用户写了多个"///"，只保留一个
        requestUri = requestUri.replace(contextPath, "").replaceAll("/+", "/");

        //遍历HandlerMapping，寻找url匹配的
        for (Map.Entry<String, HandlerModel> entry : handlerMapping.entrySet()) {
            if (entry.getKey().equals(requestUri)) {
                //取出对应的HandlerModel
                HandlerModel handlerModel = entry.getValue();

                Map<String, Integer> paramIndexMap = handlerModel.paramMap;
                //定义一个数组来保存应该给method的所有参数赋值的数组
                Object[] paramValues = new Object[paramIndexMap.size()];

                Class<?>[] types = handlerModel.method.getParameterTypes();

                //遍历一个方法的所有参数[name->0,addr->1,HttpServletRequest->2]
                for (Map.Entry<String, Integer> param : paramIndexMap.entrySet()) {
                    String key = param.getKey();
                    if (key.equals(HttpServletRequest.class.getName())) {
                        paramValues[param.getValue()] = request;
                    } else if (key.equals(HttpServletResponse.class.getName())) {
                        paramValues[param.getValue()] = response;
                    } else {
                        //如果用户传了参数，譬如 name= "wolf"，做一下参数类型转换，将用户传来的值转为方法中参数的类型
                        String parameter = request.getParameter(key);
                        if (parameter != null) {
                            paramValues[param.getValue()] = convert(parameter.trim(), types[param.getValue()]);
                        }
                    }
                }
                //激活该方法
                handlerModel.method.invoke(handlerModel.controller, paramValues);
                return true;
            }
        }

        return false;
    }
    private Object convert(String parameter, Class<?> targetType) {
        if (targetType == String.class) {
            return parameter;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(parameter);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(parameter);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (parameter.toLowerCase().equals("true") || parameter.equals("1")) {
                return true;
            } else if (parameter.toLowerCase().equals("false") || parameter.equals("0")) {
                return false;
            }
            throw new RuntimeException("不支持的参数");
        }
        else {
            //TODO 还有很多其他的类型，char、double之类的依次类推，也可以做List<>, Array, Map之类的转化
            return null;
        }
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
