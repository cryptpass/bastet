package com.arpanrec.bastet;

import com.arpanrec.bastet.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;

@Slf4j
public class Application {

    public static Config CONFIG;

    static {
        String configString = System.getenv("BASTET_CONFIG");
        if (configString == null) {
            log.info("No config provided via environment variable `BASTET_CONFIG`");
            throw new RuntimeException("No config provided");
        } else {
            log.info("Config provided via environment variable `BASTET_CONFIG`");
        }

        log.debug("Loading configString from {}", configString);
        configString = FileUtils.fileOrString(configString);
        log.debug("Loaded configString from {}", configString);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            Application.CONFIG = objectMapper.readValue(configString, Config.class);
            log.info("Loaded configString from {}", configString);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse config.", e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }
}
