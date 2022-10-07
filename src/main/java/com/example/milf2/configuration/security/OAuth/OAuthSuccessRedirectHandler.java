package com.example.milf2.configuration.security.OAuth;

import com.example.milf2.configuration.security.JwtUtil;
import com.example.milf2.domain.user.Role;
import com.example.milf2.domain.user.User;
import com.example.milf2.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.savedrequest.ServerRequestCache;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class OAuthSuccessRedirectHandler extends RedirectServerAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;


    public OAuthSuccessRedirectHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {

        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();

        if (response.isCommitted()) {
            return Mono.empty();
        }

        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        User user = new User(oidcUser.getClaim("oid"), oidcUser.getFullName());
        response.getHeaders().setLocation(URI.create("http://localhost:3000/login"));
        response.setRawStatusCode(302);


        response.addCookie(ResponseCookie.from("token", jwtUtil.generateToken(user))
                .path("/")
                .maxAge(86400)
                .httpOnly(true).build());


        response.addCookie(ResponseCookie.from("shouldFetch", "true")
                .path("/")
                .httpOnly(false)
                .maxAge(86400)
                .build());

        return Mono.empty();
    }

}
