package com.example.milf2.controller;


import com.example.milf2.configuration.security.JwtUtil;
import com.example.milf2.domain.user.User;
import com.example.milf2.repository.UserRepository;
import com.example.milf2.service.PostService;
import com.example.milf2.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@CrossOrigin(value = "http://localhost:3000", origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository, UserService userService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }


    @PostMapping("/log-out")
    public Mono<ResponseEntity> logOut(ServerWebExchange swe, Authentication authentication) {

        ResponseCookie responseCookie = ResponseCookie.from("token", null)
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();

        return Mono.just(ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .build());
    }


    private final static ResponseEntity<Object> UNAUTHORIZED =
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    @PostMapping("/login")
    public Mono<ResponseEntity> login(ServerWebExchange swe) {
        return swe.getFormData().flatMap(credentials ->
                userRepository.findByUsername(credentials.getFirst("username"))
                        .cast(User.class)
                        .map(userDetails -> {
                                    if (Objects.equals(credentials.getFirst("password"), userDetails.getPassword())) {
                                        swe.getResponse()
                                                .addCookie(
                                                        ResponseCookie.from("token", jwtUtil.generateToken(userDetails))
                                                                .path("/")
                                                                .httpOnly(true).build());
                                        return ResponseEntity.ok(userDetails);
                                    } else
                                        return UNAUTHORIZED;
                                }
                        )
                        .defaultIfEmpty(UNAUTHORIZED)
        );
    }


    @QueryMapping
    public Mono<User> getMyProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Mono.just(user);
    }

    @QueryMapping
    public Mono<User> getUserById(@Argument String id) {
        return userService.getOneById(id);
    }

}

