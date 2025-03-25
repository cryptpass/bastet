package com.arpanrec.bastet;

import com.arpanrec.bastet.exceptions.CaughtException;
import com.arpanrec.bastet.physical.Physical;
import com.arpanrec.bastet.physical.User;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Bootstrap implements CommandLineRunner {
    private final ConfigService configService;
    private final Physical physical;

    public Bootstrap(@Autowired ConfigService configService,
                     @Autowired Physical physical) {
        this.configService = configService;
        this.physical = physical;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Bastet.");
        createRootUser();
    }

    private void createRootUser() {
        Optional<User> rootUserMaybe = physical.readUser("root");
        User rootUser = rootUserMaybe.orElseGet(User::new);
        User.Privilege rootPrivilege = new User.Privilege(User.Privilege.Type.SUDO);
        User.Role rootRole = new User.Role(User.Role.Type.ADMIN, List.of(rootPrivilege));
        rootUser.setRoles(List.of(rootRole));
        rootUser.setUsername("root");
        rootUser.setEnabled(true);
        if (configService.getConfig().server().rootPassword() == null && rootUser.getPassword() == null) {
            throw new CaughtException("Root user password not set");
        }
        if (configService.getConfig().server().rootPassword() != null) {
            log.info("Resetting root user password");
            rootUser.setPassword(configService.getConfig().server().rootPassword());
        } else {
            log.info("Not resetting root user password");
        }
        physical.writeUser(rootUser);
        log.info("Root user settings complete, root user: {}", rootUser);
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        log.debug("Starting web server factory.");
        return (configurableServletWebServerFactory -> {
            ConfigService.Config.Server serverConfig = configService.getConfig().server();
            configurableServletWebServerFactory.setContextPath("");
            int port = serverConfig.port();
            if (port == 0) {
                port = 8080;
            }
            configurableServletWebServerFactory.setPort(port);
            log.info("Web server port {}", port);
            if (serverConfig.sslCertPem() != null && serverConfig.sslKeyPem() != null) {
                Ssl ssl = new Ssl();
                ssl.setCertificate(serverConfig.sslCertPem());
                ssl.setCertificatePrivateKey(serverConfig.sslKeyPem());
                configurableServletWebServerFactory.setSsl(ssl);
                log.info("Server configured for SSL");
            }
        });
    }
}
