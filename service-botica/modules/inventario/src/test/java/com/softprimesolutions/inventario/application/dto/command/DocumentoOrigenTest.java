package com.softprimesolutions.inventario.application.dto.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentoOrigenTest {

    @Test
    void theSupplierIsOptional() {
        var origen = new DocumentoOrigen("VENTA", UUID.randomUUID(), UUID.randomUUID(), null);

        assertThat(origen.proveedorId()).isNull();
    }

    @Test
    void typeDocumentAndLineAreRequired() {
        var id = UUID.randomUUID();
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen(null, id, id, null));
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen("VENTA", null, id, null));
        assertThatNullPointerException().isThrownBy(() -> new DocumentoOrigen("VENTA", id, null, null));
    }
}
