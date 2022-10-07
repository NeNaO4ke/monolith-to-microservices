package com.example.milf2.service;

import com.example.milf2.domain.user.User;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class UserService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;


    public UserService(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }


    public Flux<User> getAllUsersWithIds(Flux<String> ids) {
        return ids.flatMap(this::getOneById);
    }

    public Mono<User> getOneById(String id) {
        return reactiveMongoTemplate.findById(id, User.class);
    }

    public Mono<User> saveUser(User user){
      return reactiveMongoTemplate.save(user);
    }

}


