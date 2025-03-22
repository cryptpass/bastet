package com.arpanrec.bastet.model;

import com.arpanrec.bastet.hash.Hashing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity(name = "users_t")
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = -8372582194659560207L;

    @SuppressWarnings({"unused"})
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c")
    private Long id;

    @Setter
    @Column(name = "username_c", nullable = false, unique = true)
    private String username;

    @Getter
    @Setter
    @Column(name = "email_c")
    private String email;

    @Column(name = "password_c")
    private String password;

    @Setter
    @Getter
    @Convert(converter = UserPermissionsAttributeConverter.class)
    @Column(name = "roles_c", nullable = false, columnDefinition = "TEXT")
    private List<Role> roles;

    @Setter
    @Column(name = "account_non_expired_c", nullable = false)
    private boolean accountNonExpired = false;

    @Setter
    @Column(name = "account_non_locked_c", nullable = false)
    private boolean accountNonLocked = false;

    @Setter
    @Column(name = "credentials_non_expired_c", nullable = false)
    private boolean credentialsNonExpired = false;

    @Setter
    @Column(name = "enabled_c", nullable = false)
    private boolean enabled = false;

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    public void setPassword(String password) {
        Hashing hashing = new Hashing();
        this.password = hashing.encode(password);
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "username: " + username + ", email: " + email + ", roles: " + roles;
    }
}