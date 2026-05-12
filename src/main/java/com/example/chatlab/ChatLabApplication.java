package com.example.chatlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@SpringBootApplication
public class ChatLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatLabApplication.class, args);
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        // Force HTTP/1.1 — prevents RST_STREAM errors when talking to
        // WireMock (and many real APIs) that don't support HTTP/2
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }
}

