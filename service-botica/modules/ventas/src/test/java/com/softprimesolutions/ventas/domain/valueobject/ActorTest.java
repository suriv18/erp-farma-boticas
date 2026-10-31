package com.softprimesolutions.ventas.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void keepsTheFullUserIdAsTheActorCode() {
        var id = UUID.fromString("12345678-9abc-4def-8123-456789abcdef");

        assertThat(new Actor(id).codigo()).isEqualTo("12345678-9abc-4def-8123-456789abcdef").hasSize(36);
        assertThat(new Actor(id).id()).isEqualTo(id);
    }

    @Test
    void requiresAnId() {
        assertThatNullPointerException().isThrownBy(() -> new Actor(null));
    }
}
