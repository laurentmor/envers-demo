package com.example.enversdemo.config;

import org.jolokia.server.core.http.AgentServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Servlet;

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
