package com.arpanrec.bastet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role implements GrantedAuthority {

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
