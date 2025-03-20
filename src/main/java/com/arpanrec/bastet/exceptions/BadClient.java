package com.arpanrec.bastet.exceptions;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad client request")
public class BadClient extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -2497974244720111952L;

    public BadClient(String message) {
        super(message);
    }
}
