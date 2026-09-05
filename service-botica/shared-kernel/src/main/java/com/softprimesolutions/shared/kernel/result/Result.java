package com.softprimesolutions.shared.kernel.result;

import java.util.Objects;
import java.util.function.Function;

/**
 * Resultado explícito de una operación esperable, sin usar excepciones como flujo de negocio.
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super E, ? extends R> onFailure);

    default boolean isSuccess() {
        return fold(ignored -> true, ignored -> false);
    }

    default boolean isFailure() {
        return !isSuccess();
    }

    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper es obligatorio");
        return fold(value -> success(mapper.apply(value)), Result::failure);
    }

    default <U> Result<U, E> flatMap(
            Function<? super T, ? extends Result<U, E>> mapper) {
        Objects.requireNonNull(mapper, "mapper es obligatorio");
        return fold(mapper, Result::failure);
    }

    default <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
        Objects.requireNonNull(mapper, "mapper es obligatorio");
        return fold(Result::success, error -> failure(mapper.apply(error)));
    }

    default T getOrElse(Function<? super E, ? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback es obligatorio");
        return fold(Function.identity(), fallback);
    }

    record Success<T, E>(T value) implements Result<T, E> {

        public Success {
            Objects.requireNonNull(value, "value es obligatorio");
        }

        @Override
        public <R> R fold(
                Function<? super T, ? extends R> onSuccess,
                Function<? super E, ? extends R> onFailure) {
            Objects.requireNonNull(onSuccess, "onSuccess es obligatorio");
            Objects.requireNonNull(onFailure, "onFailure es obligatorio");
            return onSuccess.apply(value);
        }
    }

    record Failure<T, E>(E error) implements Result<T, E> {

        public Failure {
            Objects.requireNonNull(error, "error es obligatorio");
        }

        @Override
        public <R> R fold(
                Function<? super T, ? extends R> onSuccess,
                Function<? super E, ? extends R> onFailure) {
            Objects.requireNonNull(onSuccess, "onSuccess es obligatorio");
            Objects.requireNonNull(onFailure, "onFailure es obligatorio");
            return onFailure.apply(error);
        }
    }
}
