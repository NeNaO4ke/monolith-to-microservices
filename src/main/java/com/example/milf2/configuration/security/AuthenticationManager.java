package com.example.milf2.configuration.security;

import com.example.milf2.domain.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.lang.Maps;
import org.springframework.core.serializer.Deserializer;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {
    private final JwtUtil jwtUtil;

    public AuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        String username;
        try {
            username = jwtUtil.extractUsername(authToken);
        } catch (Exception e) {
            username = null;
            System.out.println(e);
        }
        if (username != null && jwtUtil.validateToken(authToken)) {
            User user = Jwts.parserBuilder()
                    .setSigningKey(Base64.getEncoder().encodeToString("very-very-secret-key-should-be-almost-infinity".getBytes()))
                    .deserializeJsonWith(new JacksonDeserializer(Maps.of("user", User.class).build())) // <-----
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody()
                    .get("user", User.class);
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    authorities
            );

            return Mono.just(authenticationToken);
        } else {
            return Mono.empty();
        }
    }
}
