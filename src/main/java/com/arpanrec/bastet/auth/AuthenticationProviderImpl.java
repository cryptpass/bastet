package com.arpanrec.bastet.auth;

import com.arpanrec.bastet.hash.Hashing;
import com.arpanrec.bastet.physical.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationProviderImpl implements AuthenticationProvider {

    private final PasswordEncoder encoder = Hashing.INSTANCE;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.trace("User authentication started for {}", authentication.getName());
        User user = (User) authentication.getDetails();
        if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked()
            || !user.isCredentialsNonExpired() || ((AuthenticationImpl) authentication).getProvidedPassword() == null) {
            authentication.setAuthenticated(false);
            log.trace("User authentication set to false for {}", authentication.getName());
            return authentication;
        }
        if (encoder.matches(((AuthenticationImpl) authentication).getProvidedPassword(),
            (String) authentication.getCredentials())) {
            log.trace("User {} authenticated", authentication.getName());
            authentication.setAuthenticated(true);
        } else {
            throw new BadCredentialsException("Wrong password");
        }
        return authentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Return true if this AuthenticationProvider supports the provided authentication class
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
