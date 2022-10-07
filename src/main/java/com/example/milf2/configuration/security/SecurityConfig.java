package com.example.milf2.configuration.security;

import com.example.milf2.configuration.security.OAuth.OAuthSuccessRedirectHandler;
import com.example.milf2.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final OAuthSuccessRedirectHandler oAuthSuccessRedirectHandler;
    private final ReactiveAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthConverter authConverter;

    public SecurityConfig(OAuthSuccessRedirectHandler oAuthSuccessRedirectHandler, ReactiveAuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository, AuthConverter authConverter) {
        this.oAuthSuccessRedirectHandler = oAuthSuccessRedirectHandler;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authConverter = authConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    private AuthenticationWebFilter jwtAuthenticationFilter() {
        AuthenticationWebFilter jwtAuthenticationFilter = new AuthenticationWebFilter(authenticationManager);
        jwtAuthenticationFilter.setSecurityContextRepository(securityContextRepository);
        jwtAuthenticationFilter.setServerAuthenticationConverter(authConverter);
        return jwtAuthenticationFilter;
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {

        return httpSecurity
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .logout()
                .logoutUrl("/log-out")
                .logoutSuccessHandler((exchange, authentication) -> logoutHandler(exchange))
                .and()
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .oauth2Login(oauth2 -> oauth2.authenticationSuccessHandler(oAuthSuccessRedirectHandler))
                .authorizeExchange()
                .pathMatchers("/login").permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .anyExchange().authenticated()
                .and()
                .build();
    }

    @NotNull
    private Mono<Void> logoutHandler(WebFilterExchange exchange) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        response.setRawStatusCode(200);
        response.getHeaders().setLocation(URI.create("http://localhost:3000/login"));
        response.getCookies().remove("SESSION");
        response.getHeaders().setAccessControlAllowOrigin("http://localhost:3000");
        response.getHeaders().setAccessControlAllowCredentials(true);
        response.addCookie(ResponseCookie.from("token", null)
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build());
        return exchange.getExchange().getSession()
                .flatMap(WebSession::invalidate);
    }


}
