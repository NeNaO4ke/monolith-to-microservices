package com.example.milf2.service;

import com.example.milf2.domain.user.Role;
import com.example.milf2.domain.user.User;
import com.example.milf2.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@Slf4j
public class CustomOidcUserService extends OidcReactiveOAuth2UserService {

    private final UserService userService;

    public CustomOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Mono<OidcUser> oidcUser = super.loadUser(userRequest);
        log.info(oidcUser.toString());
        return oidcUser.flatMap(this::updateUser);
    }

    private Mono<OidcUser> updateUser(OidcUser oidcUser) {
        User user = new User(oidcUser.getClaim("oid"), oidcUser.getFullName());
        return userService.getOneById(user.getId())
                .switchIfEmpty(Mono.defer(() -> userService.saveUser(user)))
                .flatMap(user1 -> Mono.just(oidcUser));
    }
}
