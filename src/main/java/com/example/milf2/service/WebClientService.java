package com.example.milf2.service;

import com.example.milf2.domain.Dto.MetaDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class WebClientService {


    WebClient client = WebClient.create("http://localhost:8081");


    public Mono<MetaDto> getMetaDtoMono(String url) {
        return client.get()
                .uri("/parse-html?url=" + url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MetaDto.class);
    }


}
