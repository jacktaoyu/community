package com.ty.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * 2.5.1.编辑工具类
 * 在util包中添加CookieUtil工具类提供获得Cookie值的方法，代码见下。
 */
public class CookieUtil {
    public static String getValue(HttpServletRequest request, String name){
        if(request==null || name==null){
            throw new IllegalArgumentException("参数为空");
        }
        Cookie[] cookies=request.getCookies();
        if(cookies!=null){
            for(Cookie cookie:cookies){
                if(cookie.getName().equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
