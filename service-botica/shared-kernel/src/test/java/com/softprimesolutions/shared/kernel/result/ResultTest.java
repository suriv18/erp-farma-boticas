package com.softprimesolutions.shared.kernel.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void mapsASuccessWithoutLosingItsType() {
        Result<Integer, String> result = Result.success(20);

        var mapped = result.map(value -> value * 2);

        assertTrue(mapped.isSuccess());
        assertEquals(40, mapped.getOrElse(error -> 0));
    }

    @Test
    void preservesAFailureWhenMapping() {
        Result<Integer, String> result = Result.failure("stock.insuficiente");

        var mapped = result.map(value -> value * 2);

        assertTrue(mapped.isFailure());
        assertEquals(-1, mapped.getOrElse(error -> -1));
    }
}
