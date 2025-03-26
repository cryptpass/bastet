package com.arpanrec.bastet.auth;

import com.arpanrec.bastet.ConfigService;
import com.arpanrec.bastet.physical.Physical;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private String headerKey = "Authorization";
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthenticationFilter(@Autowired AuthenticationManagerImpl authenticationManagerImpl,
                                @Autowired Physical physical, @Autowired ConfigService configService) {
        String headerKey = configService.getConfig().server().authHeaderKey();
        if (headerKey != null && !headerKey.isBlank()) {
            this.headerKey = headerKey;
        }
        this.authenticationManager = authenticationManagerImpl;
        this.userDetailsService = physical;
    }

    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        AuthenticationImpl authentication = new AuthenticationImpl();
        String token = request.getHeader(headerKey);
        if (token != null) {
            setCredentials(token, authentication);
        }
        Authentication authenticated = authenticationManager.authenticate(authentication);
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        filterChain.doFilter(request, response);
    }

    private void setCredentials(String basicAuthToken, AuthenticationImpl authentication) {
        try {
            log.trace("basicAuthToken: {}", basicAuthToken);
            String[] credential = new String(Base64.getDecoder().decode(basicAuthToken.substring(6))).split(":");
            String username = credential[0];
            String providedPassword = credential[1];
            authentication.setProvidedPassword(providedPassword);
            UserDetails user = userDetailsService.loadUserByUsername(username);
            authentication.setUser(user);
        } catch (Exception e) {
            log.error("Error while decoding basic auth token", e);
        }
    }
}
