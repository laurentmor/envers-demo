package com.example.enversdemo.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.sql.SQLException;

@Configuration
public class DBConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        System.out.println("Starting H2 TCP server on port 9092...");
        return Server.createTcpServer("-tcpAllowOthers", "-tcpPort", "9092");
    }
}

// jdbc:h2:tcp://localhost:9092/mem:enversdemo