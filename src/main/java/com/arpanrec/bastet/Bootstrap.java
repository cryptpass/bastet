package com.arpanrec.bastet;


import com.arpanrec.bastet.exceptions.CaughtException;
import com.arpanrec.bastet.model.Privilege;
import com.arpanrec.bastet.model.Role;
import com.arpanrec.bastet.model.User;
import com.arpanrec.bastet.services.UserService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Bootstrap implements CommandLineRunner {

    private final UserService userService;
    private final String rootUserPassword;

    public Bootstrap(@Autowired UserService userService,
                     @Value("${bastet.root-password:#{null}}") String rootUserPassword) {
        this.rootUserPassword = rootUserPassword;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting bootstrap");
        try {
            this.createRootUser();
        } catch (CaughtException e) {
            throw new Exception("Bootstrap failed", e);
        }
        log.info("Bootstrap complete");
    }

    private void createRootUser() {
        Optional<User> rootUserMaybe = userService.findByUsername("root");
        User rootUser = rootUserMaybe.orElseGet(User::new);
        Privilege rootPrivilege = new Privilege(Privilege.Type.SUDO);
        Role rootRole = new Role(Role.Type.ADMIN, List.of(rootPrivilege));
        rootUser.setRoles(List.of(rootRole));
        rootUser.setUsername("root");
        rootUser.setEnabled(true);
        rootUser.setAccountNonExpired(true);
        rootUser.setAccountNonLocked(true);
        rootUser.setCredentialsNonExpired(true);
        if (rootUserPassword == null && rootUser.getPassword() == null) {
            throw new CaughtException("Root user password not set");
        }
        if (rootUserPassword != null) {
            log.info("Resetting root user password");
            rootUser.setPassword(rootUserPassword);
        } else {
            log.info("Not resetting root user password");
        }
        userService.saveUser(rootUser);
        log.info("Root user settings complete, root user: {}", rootUser);
    }
}
