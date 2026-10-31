package com.softprimesolutions.compras.domain.valueobject;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void exposesTheIdentifierAsItsCode() {
        assertThat(new Actor(ACTOR_ID).codigo()).isEqualTo(ACTOR_ID.toString());
        assertThat(new Actor(ACTOR_ID).id()).isEqualTo(ACTOR_ID);
    }

    @Test
    void requiresAnIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new Actor(null));
    }
}
