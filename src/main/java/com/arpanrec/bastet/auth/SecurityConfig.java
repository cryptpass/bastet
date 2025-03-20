package com.arpanrec.bastet.auth;

import com.arpanrec.bastet.model.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OncePerRequestFilter authenticationOncePerRequestFilter;

    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(@Autowired AuthenticationFilter authenticationFilter,
                          @Autowired AuthenticationProviderImpl authenticationProviderImpl) {
        this.authenticationOncePerRequestFilter = authenticationFilter;
        this.authenticationProvider = authenticationProviderImpl;
    }

    private RequestMatcher[] getPermitAllRequestMatchers() {
        return new RequestMatcher[]{
            new AntPathRequestMatcher("/h2-console/**"), new AntPathRequestMatcher("/error"),
            new AntPathRequestMatcher("/actuator/**"), new AntPathRequestMatcher("/error")
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.cors(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        http.sessionManagement(
            sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
        http.authenticationProvider(authenticationProvider);

        http.addFilterAfter(authenticationOncePerRequestFilter, BasicAuthenticationFilter.class);

        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
            .requestMatchers(getPermitAllRequestMatchers()).permitAll()
            .requestMatchers(new AntPathRequestMatcher("/api/v1/admin/**")).hasAuthority("ROLE_" + Role.Type.ADMIN.name())
            .anyRequest().authenticated()
        );

        return http.build();
    }
}
