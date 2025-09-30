package edu.qhjy.score_service.aop;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保这是第一个执行的 Filter
public class PerformanceLoggingFilter implements Filter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long startTime = System.currentTimeMillis(); // 改为毫秒，方便阅读
        HttpServletRequest req = (HttpServletRequest) request;

        log.debug("【计时点 A - Filter 开始】请求 [{} {}] 进入应用...", req.getMethod(), req.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("【计时点 D - Filter 结束】请求 [{} {}] 离开应用。Filter总耗时: {}ms", req.getMethod(), req.getRequestURI(), duration);
        }
    }
}