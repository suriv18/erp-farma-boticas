package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.ventas.application.port.out.ActorActualPort;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextActorAdapter implements ActorActualPort {

    @Override
    public UUID actorId() {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
