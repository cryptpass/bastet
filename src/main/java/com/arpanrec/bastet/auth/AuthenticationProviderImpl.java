package com.arpanrec.bastet.auth;

import com.arpanrec.bastet.hash.Hashing;
import com.arpanrec.bastet.physical.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationProviderImpl implements AuthenticationProvider {

    private final PasswordEncoder encoder = Hashing.INSTANCE;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.getDetails() == null) {
            authentication.setAuthenticated(false);
            return authentication;
        }

        log.trace("User authentication started for {}", authentication.getName());
        UserDetails user = (User) authentication.getDetails();
        CharSequence providedPassword = ((AuthenticationImpl) authentication).getProvidedPassword();

        if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked()
            || !user.isCredentialsNonExpired() || providedPassword == null) {
            authentication.setAuthenticated(false);
            log.trace("User authentication set to false for {}", authentication.getName());
            return authentication;
        }
        if (encoder.matches(providedPassword, (String) authentication.getCredentials())) {
            log.trace("User {} authenticated", authentication.getName());
            authentication.setAuthenticated(true);
        } else {
            log.trace("User {} authentication failed", authentication.getName());
            authentication.setAuthenticated(false);
        }
        return authentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Return true if this AuthenticationProvider supports the provided authentication class
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
