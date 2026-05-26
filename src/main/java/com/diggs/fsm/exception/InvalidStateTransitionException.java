// src/main/java/com/diggs/exception/InvalidStateTransitionException.java
package com.diggs.fsm.exception;

public class InvalidStateTransitionException extends Exception {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
    
    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}