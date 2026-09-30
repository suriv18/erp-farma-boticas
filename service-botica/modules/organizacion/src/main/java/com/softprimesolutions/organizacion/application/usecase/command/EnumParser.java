package com.softprimesolutions.organizacion.application.usecase.command;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

final class EnumParser {

    private EnumParser() {
    }

    static <E extends Enum<E>> Optional<E> parse(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst();
    }

    static <E extends Enum<E>> String allowedValues(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
