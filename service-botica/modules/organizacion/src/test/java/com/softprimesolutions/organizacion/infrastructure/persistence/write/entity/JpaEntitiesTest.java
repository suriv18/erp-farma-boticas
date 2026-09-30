package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class JpaEntitiesTest {

    private static final List<Class<?>> ENTITIES = List.of(
            EmpresaOperadoraJpaEntity.class, EstablecimientoJpaEntity.class,
            AlmacenJpaEntity.class, TerminalPosJpaEntity.class);

    @Test
    void exposesEveryGetterOnInstancesBuiltByTheProtectedConstructor() throws Exception {
        for (var type : ENTITIES) {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            var instance = constructor.newInstance();

            var getters = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .filter(method -> method.getParameterCount() == 0)
                    .toList();
            for (var getter : getters) {
                getter.invoke(instance);
            }

            assertThat(getters).isNotEmpty();
        }
    }
}
