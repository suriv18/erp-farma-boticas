package com.softprimesolutions.shared.application.port;

import java.util.UUID;

@FunctionalInterface
public interface IdentifierGenerator {

    UUID next();
}
