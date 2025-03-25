package com.arpanrec.bastet;

import com.arpanrec.bastet.physical.Physical;
import com.arpanrec.bastet.utils.FileUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Getter
public class ConfigService implements Serializable {

    private final Config config;

    public ConfigService(@Value("${bastet-config:#{null}}") String configString) {
        if (configString == null) {
            log.info("No config provided via application properties");

            configString = System.getenv("BASTET_CONFIG");
            if (configString == null) {
                log.info("No config provided via environment variable `BASTET_CONFIG`");
                throw new RuntimeException("No config provided");
            } else {
                log.info("Config provided via environment variable `BASTET_CONFIG`");
            }
        } else {
            log.info("Config provided via application properties");
        }

        log.debug("Loading configString from {}", configString);
        configString = FileUtils.fileOrString(configString);
        log.debug("Loaded configString from {}", configString);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            this.config = objectMapper.readValue(configString, Config.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse config.", e);
        }
    }

    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public record Config(Physical.Config physical, ConfigService.Config.Server server) implements Serializable {

        @Serial
        private static final long serialVersionUID = -3125980127858127864L;

        public record Server(int port,
                             @JsonProperty("root-password") String rootPassword,
                             @JsonProperty("auth-header-key") String authHeaderKey,
                             @JsonProperty("ssl-key-pem") String sslKeyPem,
                             @JsonProperty("ssl-cert-pem") String sslCertPem) implements Serializable {
            @Serial
            private static final long serialVersionUID = -31124125128127864L;
        }
    }
}
