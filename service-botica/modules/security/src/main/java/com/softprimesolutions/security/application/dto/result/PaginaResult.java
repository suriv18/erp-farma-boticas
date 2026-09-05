package com.softprimesolutions.security.application.dto.result;

import java.util.List;

public record PaginaResult<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResult {
        items = List.copyOf(items);
    }
}
