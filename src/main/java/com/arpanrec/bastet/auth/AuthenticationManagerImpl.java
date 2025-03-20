package com.arpanrec.bastet.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationManagerImpl implements AuthenticationManager {

    private final AuthenticationProvider authenticationProvider;

    public AuthenticationManagerImpl(@Autowired AuthenticationProviderImpl authenticationProviderImpl) {
        this.authenticationProvider = authenticationProviderImpl;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return authenticationProvider.authenticate(authentication);
    }
}
