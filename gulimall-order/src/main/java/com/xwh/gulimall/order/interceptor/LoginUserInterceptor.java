package com.xwh.gulimall.order.interceptor;


import com.xwh.common.constant.AuthServerConstant;
import com.xwh.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        if (match) {
            return true;
        }
        MemberRespVo o = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (new AntPathMatcher().match("/order/order/status/**", request.getRequestURI())
                || new AntPathMatcher().match("/payed/**", request.getRequestURI())
                || new AntPathMatcher().match("/my/**", request.getRequestURI())) {
            return true;
        }
        if (o != null) {
            threadLocal.set(o);
            return true;
        } else {
//            return  true;
            request.getSession().setAttribute("message", "请先进行登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
