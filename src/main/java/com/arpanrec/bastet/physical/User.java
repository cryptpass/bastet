package com.arpanrec.bastet.physical;

import com.arpanrec.bastet.hash.Hashing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User implements Serializable, UserDetails, CredentialsContainer {

    @JsonIgnore
    private static final long EXPIRATION = 7L * 24L * 60L * 60L * 1000L;

    @JsonIgnore
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Serial
    private static final long serialVersionUID = -8372582194659560207L;

    @JsonIgnore
    private String username;

    private String email;

    @JsonIgnore
    private String passwordHash;

    private long passwordLastChanged;

    private List<Role> roles;

    private long lastLogin;

    private boolean locked;

    private boolean enabled;

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
        this.passwordLastChanged = System.currentTimeMillis();
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordLastChanged = System.currentTimeMillis();
    }

    public void setPassword(String password) {
        Hashing hashing = Hashing.INSTANCE;
        this.passwordHash = hashing.encode(password);
        this.passwordLastChanged = System.currentTimeMillis();
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return System.currentTimeMillis() - passwordLastChanged < EXPIRATION;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        if (this.lastLogin == 0L) {
            return true;
        }
        return System.currentTimeMillis() - this.lastLogin < EXPIRATION;
    }

    @Override
    public String toString() {
        return "username: " + username + ", email: " + email + ", passwordHash: " + passwordHash + ", " +
            "passwordLastChanged: " + new Date(passwordLastChanged) + ", roles: " + roles + ", lastLogin: " + new Date(lastLogin) + ", locked: " + locked + ", enabled: " + enabled;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Role implements GrantedAuthority {

        @Serial
        private static final long serialVersionUID = 989235129409752804L;

        private Type name;

        private Collection<Privilege> privileges;

        @JsonIgnore
        @Override
        public String getAuthority() {
            return "ROLE_" + name.toString();
        }

        public enum Type {
            ADMIN, USER
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Privilege implements GrantedAuthority {
        @Serial
        private static final long serialVersionUID = -1453442487053691797L;

        private Type name;

        @Override
        @JsonIgnore
        public String getAuthority() {
            return name.toString();
        }

        public enum Type {
            SUDO
        }
    }

    @JsonIgnore
    public String getRolesString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this.roles);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert list of roles to JSON string", e);
        }
    }

    public void setRolesString(String dbData) {
        try {
            this.roles = OBJECT_MAPPER.readValue(dbData,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Role.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON string to list of roles", e);
        }
    }
}
