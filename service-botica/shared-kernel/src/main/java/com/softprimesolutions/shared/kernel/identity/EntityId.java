package com.softprimesolutions.shared.kernel.identity;

/** Contrato para identificadores fuertes definidos por cada bounded context. */
public interface EntityId<T> {

    T value();
}
