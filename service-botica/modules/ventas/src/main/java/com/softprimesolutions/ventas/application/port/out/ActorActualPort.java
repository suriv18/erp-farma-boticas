package com.softprimesolutions.ventas.application.port.out;

import java.util.UUID;

@FunctionalInterface
public interface ActorActualPort {

    UUID actorId();
}
