package com.arpanrec.bastet.exceptions;

import java.io.Serial;

public class PhysicalException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 24979720111952L;

    public PhysicalException(String message) {
        super(message);
    }

    public PhysicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
