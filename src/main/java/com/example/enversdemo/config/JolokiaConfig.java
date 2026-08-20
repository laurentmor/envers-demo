package com.example.enversdemo.config;

import jakarta.servlet.Servlet;
import org.jolokia.http.AgentServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JolokiaConfig {

    @Bean
    public ServletRegistrationBean<Servlet> jolokiaServlet() {
        ServletRegistrationBean<Servlet> reg = new ServletRegistrationBean<>(new AgentServlet(), "/jolokia/*");
        reg.setName("JolokiaServlet");
        reg.setLoadOnStartup(1);
        return reg;
    }
}
