package com.arpanrec.bastet.auth;

import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.security.auth.Subject;
import java.io.Serial;
import java.util.Collection;

@Data
public class AuthenticationImpl implements Authentication {

    @Serial
    private static final long serialVersionUID = -8620294545092862085L;

    private boolean authenticated;

    private UserDetails user;

    private CharSequence providedPassword;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user == null) {
            return null;
        }
        return user.getAuthorities();
    }

    @Override
    public Object getCredentials() {
        if (user == null) {
            return null;
        }
        return this.user.getPassword();
    }

    @Override
    public Object getDetails() {
        return user;
    }

    @Override
    public Object getPrincipal() {
        if (user == null) {
            return null;
        }
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
        if (user == null) {
            return null;
        }
        return this.user.getUsername();
    }

    @Override
    public boolean implies(Subject subject) {
        return Authentication.super.implies(subject);
    }
}
