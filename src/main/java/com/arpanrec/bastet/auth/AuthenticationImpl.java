package com.arpanrec.bastet.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.security.auth.Subject;
import java.io.Serial;
import java.util.Collection;

@Builder
public class AuthenticationImpl implements Authentication {

    @Serial
    private static final long serialVersionUID = -8620294545092862085L;

    private boolean authenticated;

    private UserDetails user;

    @Getter
    @Setter
    private CharSequence providedPassword;

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    public Object getCredentials() {
        return this.user.getPassword();
    }

    public Object getDetails() {
        return user;
    }

    public Object getPrincipal() {
        return this.user.getUsername();
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return this.user.getUsername();
    }

    @Override
    public boolean implies(Subject subject) {
        return Authentication.super.implies(subject);
    }
}
