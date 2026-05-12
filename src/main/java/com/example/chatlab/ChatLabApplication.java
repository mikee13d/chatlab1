package com.example.chatlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class ChatLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatLabApplication.class, args);
    }
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}


