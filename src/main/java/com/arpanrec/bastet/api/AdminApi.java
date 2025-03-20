package com.arpanrec.bastet.api;

import com.arpanrec.bastet.exceptions.BadClient;
import com.arpanrec.bastet.model.User;
import com.arpanrec.bastet.services.EncryptedEncryptionKeyService;
import com.arpanrec.bastet.services.KeyValueService;
import com.arpanrec.bastet.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/admin", produces = {MediaType.ALL_VALUE}, consumes = {MediaType.ALL_VALUE})
public class AdminApi {

    private final EncryptedEncryptionKeyService encryptedEncryptionKeyService;
    private final UserService userService;
    private final KeyValueService keyValueService;

    public AdminApi(@Autowired EncryptedEncryptionKeyService encryptedEncryptionKeyService,
                    @Autowired UserService userService,
                    @Autowired KeyValueService keyValueService) {
        this.encryptedEncryptionKeyService = encryptedEncryptionKeyService;
        this.userService = userService;
        this.keyValueService = keyValueService;
    }

    @PostMapping(path = "/unlock", consumes = {MediaType.TEXT_PLAIN_VALUE})
    public HttpEntity<?> unlock(@RequestBody String body) {
        encryptedEncryptionKeyService.setUnlockKey(body);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(path = "/user", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes =
        {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<User> createUser(@RequestBody User user) {
        userService.saveUser(user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/user/{username}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<User> readUser(@PathVariable String username) {
        User user = (User) userService.loadUserByUsername(username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping(path = "/user/{username}", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes =
        {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<User> updateUser(@PathVariable String username, @RequestBody User userUpdates) {
        if (userUpdates.getUsername() != null && username.equals(userUpdates.getUsername())) {
            throw new BadClient("Changing username is not allowed");
        }
        User user = (User) userService.loadUserByUsername(username);
        if (userUpdates.getPassword() != null) {
            user.setPassword(userUpdates.getPassword());
        }
        if (userUpdates.getRoles() != null) {
            user.setRoles(userUpdates.getRoles());
        }
        if (userUpdates.getEmail() != null) {
            user.setEmail(userUpdates.getEmail());
        }
        user.setEnabled(userUpdates.isEnabled());
        user.setAccountNonLocked(userUpdates.isAccountNonLocked());
        user.setAccountNonExpired(userUpdates.isAccountNonExpired());
        user.setCredentialsNonExpired(userUpdates.isCredentialsNonExpired());
        userService.saveUser(user);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping(path = "/list/keys/**", produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<List<String>> listKeys(HttpServletRequest request) {
        String key =
            new AntPathMatcher().extractPathWithinPattern(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(), request.getRequestURI());
        log.info("Listing keys for {}", key);
        var keys = keyValueService.list(key);
        return new ResponseEntity<>(keys, HttpStatus.OK);
    }
}
