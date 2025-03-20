package com.arpanrec.bastet.api;

import com.arpanrec.bastet.exceptions.CaughtException;
import com.arpanrec.bastet.services.KeyValueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/secrets", produces = {MediaType.ALL_VALUE}, consumes = {MediaType.ALL_VALUE})
public class SecretsApi {

    private final KeyValueService keyValueService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecretsApi(@Autowired KeyValueService keyValueService) {
        this.keyValueService = keyValueService;
    }

    @GetMapping(path = "/**", produces = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> read(HttpServletRequest request) {
        String secret =
            new AntPathMatcher().extractPathWithinPattern(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(), request.getRequestURI());
        log.info("Reading secret {}", secret);
        var kv = keyValueService.get(secret);
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(kv.getValue(), new TypeReference<>() {
            });
            return new ResponseEntity<>(jsonMap, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new CaughtException("Error parsing JSON", e);
        }
    }

    @PutMapping(path = "/**", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<?> write(@RequestBody String body, HttpServletRequest request) {
        String secret =
            new AntPathMatcher().extractPathWithinPattern(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(), request.getRequestURI());
        log.info("Writing secret: {}", secret);
        keyValueService.save(secret, body);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping(path = "/**")
    public HttpEntity<?> delete(HttpServletRequest request) {
        String secret =
            new AntPathMatcher().extractPathWithinPattern(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(), request.getRequestURI());
        log.info("Deleting secret: {}", secret);
        keyValueService.delete(secret);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
