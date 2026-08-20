package com.example.enversdemo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class JolokiaSecurityFilter {

    @Value("${management.jolokia.allowed-ips:127.0.0.1,::1,localhost}")
    private String allowedIpsProp;

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> jolokiaFilterRegistration() {
        var filter = new OncePerRequestFilter() {
            private List<String> allowed = null;

            private void ensureParsed() {
                if (allowed == null) {
                    allowed = Arrays.stream(allowedIpsProp.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                }
            }

            private boolean isAllowed(String remote) {
                try {
                    ensureParsed();
                    InetAddress addr = InetAddress.getByName(remote);
                    if (addr.isLoopbackAddress()) return allowed.stream().anyMatch(a -> a.equalsIgnoreCase("localhost") || a.equals("127.0.0.1") || a.equals("::1"));
                    return allowed.contains(remote);
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                String path = request.getRequestURI();
                if (path.startsWith(request.getContextPath() + "/jolokia") || path.startsWith("/jolokia")) {
                    String xf = request.getHeader("X-Forwarded-For");
                    String remote = xf != null && !xf.isBlank() ? xf.split(",")[0].trim() : request.getRemoteAddr();
                    if (!isAllowed(remote)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to Jolokia is restricted by IP");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
            }
        };

        var reg = new FilterRegistrationBean<OncePerRequestFilter>(filter);
        reg.addUrlPatterns("/jolokia/*");
        reg.setName("JolokiaIpFilter");
        reg.setOrder(0);
        return reg;
    }
}
