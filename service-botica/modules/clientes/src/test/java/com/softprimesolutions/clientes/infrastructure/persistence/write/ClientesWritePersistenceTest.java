package com.softprimesolutions.clientes.infrastructure.persistence.write;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.domain.model.EstadoCliente;
import com.softprimesolutions.clientes.domain.model.TipoCliente;
import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import com.softprimesolutions.clientes.infrastructure.persistence.write.entity.ClienteJpaEntity;
import com.softprimesolutions.clientes.infrastructure.persistence.write.mapper.ClientesWriteMapper;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientesWritePersistenceTest {

    @Test
    void mapsTheAggregateToItsEntityWithTheTenantPrimaryKey() {
        var id = UUID.randomUUID();
        var now = Instant.parse("2026-10-30T15:00:00Z");
        var cliente = Cliente.restore(
                new ClienteId(id), new TenantId(UUID.randomUUID()), TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, "ana@correo.pe", "999", LocalDate.parse("1990-05-10"),
                EstadoCliente.BLOQUEADO, "Fraude", now, null);

        var entity = ClientesWriteMapper.toEntity(cliente, 7L);

        assertThat(entity.getUuidPublico()).isEqualTo(id);
        assertThat(entity.getTenantId()).isEqualTo(7L);
        assertThat(entity.getTipoCliente()).isEqualTo("NATURAL");
        assertThat(entity.getTipoDocumento()).isEqualTo("1");
        assertThat(entity.getNumeroDocumento()).isEqualTo("12345678");
        assertThat(entity.getNombres()).isEqualTo("Ana");
        assertThat(entity.getApellidos()).isEqualTo("Pérez");
        assertThat(entity.getRazonSocial()).isNull();
        assertThat(entity.getEmail()).isEqualTo("ana@correo.pe");
        assertThat(entity.getTelefono()).isEqualTo("999");
        assertThat(entity.getFechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
        assertThat(entity.getEstado()).isEqualTo("BLOQUEADO");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void exposesEveryGetterOnAnInstanceBuiltByTheProtectedConstructor() throws Exception {
        var constructor = ClienteJpaEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var instance = constructor.newInstance();

        var getters = Arrays.stream(ClienteJpaEntity.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getParameterCount() == 0)
                .toList();
        for (var getter : getters) {
            getter.invoke(instance);
        }

        assertThat(getters).hasSize(14);
    }
}
