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

    private final String headerKey;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthenticationFilter(@Autowired AuthenticationManagerImpl authenticationManagerImpl,
                                @Autowired Physical physical,
                                @Autowired ConfigService configService) {
        String headerKey = configService.getConfig().server().authHeaderKey();
        if (headerKey == null) {
            headerKey = "Authorization";
        }
        this.headerKey = headerKey;
        this.authenticationManager = authenticationManagerImpl;
        this.userDetailsService = physical;
    }

    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        var base64Cred = request.getHeader(headerKey);
        if (base64Cred == null) {
            filterChain.doFilter(request, response);
            return;
        }
        log.trace("headerKey: {}, base64Cred: {}", headerKey, base64Cred);

        String[] credential = new String(Base64.getDecoder().decode(base64Cred.substring(6))).split(":");
        String username = credential[0];
        String providedPassword = credential[1];

        UserDetails user = userDetailsService.loadUserByUsername(username);

        Authentication authenticated = authenticationManager
            .authenticate(
                AuthenticationImpl
                    .builder()
                    .providedPassword(providedPassword)
                    .user(user)
                    .build()
            );

        SecurityContextHolder.getContext().setAuthentication(authenticated);

        filterChain.doFilter(request, response);
    }
}
