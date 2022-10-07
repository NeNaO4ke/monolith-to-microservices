package com.example.milf2.configuration;

import com.example.milf2.domain.Dto.PostNUser;
import com.example.milf2.domain.Subscriptions.SubscriptionEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubscriptionsSinks {


    @Bean
    public Sinks.Many<PostNUser> postCreatedSink(){
        return Sinks.many().multicast().onBackpressureBuffer(Integer.MAX_VALUE,false);
    }

    ConcurrentHashMap<String, Sinks.Many<SubscriptionEvent>> concurrentHashMap = new ConcurrentHashMap<>();

    @Bean
    public ConcurrentHashMap<String, Sinks.Many<SubscriptionEvent>> sinksHashMap(){
        return concurrentHashMap;
    }

    @Bean
    RouterFunction<ServerResponse> staticResourceRouter(){
        return RouterFunctions.resources("/**", new ClassPathResource("static/"));
    }

}
