package com.softprimesolutions.catalogo.api.dto.response;

import java.util.List;

public record PaginaResponse<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResponse {
        items = List.copyOf(items);
    }
}
