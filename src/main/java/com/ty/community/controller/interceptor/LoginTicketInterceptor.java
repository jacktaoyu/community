package com.ty.community.controller.interceptor;

import com.ty.community.entity.LoginTicket;
import com.ty.community.entity.User;
import com.ty.community.service.UserService;
import com.ty.community.util.CookieUtil;
import com.ty.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 2.5.4.controller层
 * 添加interceptor包，创建LoginTicketInterceptor类。
 *
 * 在LoginTicketInterceptor拦截器作用影响的请求范围内，每发送一次请求，LoginTicketInterceptor会拦截请求，
 * 先执行preHandle方法，再执行postHandle方法，再执行请求对应的控制器方法，最后执行afterCompletion方法。
 *
 * preHandle方法：获取cookie的loginticket的ticket字段，在loginticket MySQL数据库中查询loginticket实体，
 * 判断是否能查到凭证，凭证是否有效及过期，如果凭证合法则根据凭证的user_id字段查询user MySQL数据库得到user实体类，
 * 将user实体类存至hostHolder中；
 *
 * postHandle方法：从hostHolder获取user实体类，再将user实体类添加到modelAndview中，这样整个请求都持有用户信息。
 *
 * afterCompletion方法：收尾，一次请求结束后，将hostHolder中的用户信息清除。
 */
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证
            //一般不会检测到过期，因为时间一到，cookie自动被销毁了？？？？？
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户
                hostHolder.setUser(user);
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }
}

