package com.arpanrec.bastet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Privilege implements GrantedAuthority {
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
