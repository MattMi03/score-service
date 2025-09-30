package edu.qhjy.score_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TimingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TimingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute("interceptorStartTime", startTime);
        log.debug("【计时点 B - Interceptor preHandle】请求即将进入Controller...");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        long startTime = (Long) request.getAttribute("interceptorStartTime");
        long duration = System.currentTimeMillis() - startTime;
        log.debug("【计时点 C - Interceptor afterCompletion】请求处理完毕。Interceptor总耗时: {}ms", duration);
    }
}