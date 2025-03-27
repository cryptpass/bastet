package com.arpanrec.bastet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;

public record Config(int port,
                     @JsonProperty("root-password") String rootPassword,
                     @JsonProperty("auth-header-key") String authHeaderKey,
                     @JsonProperty("ssl-key-pem") String sslKeyPem,
                     @JsonProperty("ssl-cert-pem") String sslCertPem,
                     @JsonProperty("data-dir") String dataDir) implements Serializable {

    @Serial
    private static final long serialVersionUID = -3125980127858127864L;
}
