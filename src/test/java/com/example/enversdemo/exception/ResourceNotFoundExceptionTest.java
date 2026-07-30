package com.example.enversdemo.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ResourceNotFoundExceptionTest {

    @Test
    void exceptionStoresMessageAndIsRuntimeException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("missing");

        assertEquals("missing", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
