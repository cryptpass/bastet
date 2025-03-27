package com.arpanrec.bastet;

import com.arpanrec.bastet.exceptions.CaughtException;
import com.arpanrec.bastet.physical.Physical;
import com.arpanrec.bastet.physical.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.community.dialect.SQLiteDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.sqlite.JDBC;

@Slf4j
@Component
@Configuration
@EnableConfigurationProperties
@SpringBootApplication
public class Bootstrap implements CommandLineRunner {
    private final Physical physical;

    public Bootstrap(@Autowired Physical physical) {
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
        if (Application.CONFIG.rootPassword() == null && rootUser.getPassword() == null) {
            throw new CaughtException("Root user password not set");
        }
        if (Application.CONFIG.rootPassword() != null) {
            log.info("Resetting root user password");
            rootUser.setPassword(Application.CONFIG.rootPassword());
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
            configurableServletWebServerFactory.setContextPath("");
            int port = Application.CONFIG.port();
            if (port == 0) {
                port = 8080;
            }
            configurableServletWebServerFactory.setPort(port);
            log.info("Web server port {}", port);
            if (Application.CONFIG.sslCertPem() != null && Application.CONFIG.sslKeyPem() != null) {
                Ssl ssl = new Ssl();
                ssl.setCertificate(Application.CONFIG.sslCertPem());
                ssl.setCertificatePrivateKey(Application.CONFIG.sslKeyPem());
                configurableServletWebServerFactory.setSsl(ssl);
                log.info("Server configured for SSL");
            }
        });
    }


    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(true);
        hikariConfig.setIsolateInternalQueries(true);
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setDriverClassName(JDBC.class.getName());
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + Application.CONFIG.dbPath());
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(Bootstrap.class.getPackageName());
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", SQLiteDialect.class.getName());
        em.setJpaPropertyMap(properties);
        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
}
