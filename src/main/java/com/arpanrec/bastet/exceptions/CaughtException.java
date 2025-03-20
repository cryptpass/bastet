package com.arpanrec.bastet.exceptions;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CaughtException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 4029537891117232440L;

    public CaughtException(String message) {
        super(message);
    }

    public CaughtException(String message, Throwable cause) {
        super(message, cause);
    }

}
