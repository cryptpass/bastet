package com.arpanrec.bastet.api;

import com.arpanrec.bastet.exceptions.BadClient;
import com.arpanrec.bastet.physical.Physical;
import com.arpanrec.bastet.physical.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/admin", produces = {MediaType.ALL_VALUE}, consumes = {MediaType.ALL_VALUE})
public class AdminApi {

    private final Physical physical;

    public AdminApi(@Autowired Physical physical) {
        this.physical = physical;
    }

    @PutMapping(path = "/unlock", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> unlock(@RequestBody Map<String, Object> unlockKey) {
        if (unlockKey.get("key") == null) {
            throw new BadClient("Key not provided");
        }
        physical.setMasterKey((String) unlockKey.get("key"));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/user/{username}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<User> readUser(@PathVariable String username) {
        User user = physical.readUser(username).orElseThrow(
            () -> new BadClient("User not found")
        );
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping(path = "/user/{username}", produces = {MediaType.APPLICATION_JSON_VALUE},
        consumes = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<User> updateUser(@PathVariable String username,
                                       @RequestBody Map<String, Object> userDetails) {
        if (userDetails.get("username") != null || userDetails.get("passwordHash") != null || userDetails.get(
            "passwordLastChanged") != null || userDetails.get("lastLogin") != null) {
            throw new BadClient(
                "Setting passwordHash is not allowed, it is generated, set password instead. "
                    + "Setting passwordLastChanged is not allowed, it is generated. "
                    + "Setting lastLogin is not allowed, it is generated. "
                    + "Setting username is not allowed, it is immutable."
            );
        }

        userDetails.put("username", username);

        physical.writeUser(userDetails);
        User user = physical.readUser(username).orElseThrow(
            () -> new BadClient("User not found")
        );
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping(path = "/list/keys/**", produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<List<String>> listKeys(HttpServletRequest request) {
        String key = new AntPathMatcher()
            .extractPathWithinPattern(
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(),
                request.getRequestURI()
            );
        log.info("Listing keys for {}", key);
        var keys = physical.listKeys(key);
        return new ResponseEntity<>(keys, HttpStatus.OK);
    }
}
