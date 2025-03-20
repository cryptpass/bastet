package com.arpanrec.bastet.exceptions;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Key value not found")
public class KeyValueNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 576269095896683022L;

    public KeyValueNotFoundException(String message) {
        super(message);
    }

}
