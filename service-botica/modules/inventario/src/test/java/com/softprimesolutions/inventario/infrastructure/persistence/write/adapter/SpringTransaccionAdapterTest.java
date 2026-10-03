package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

class SpringTransaccionAdapterTest {

    private final SimpleTransactionStatus status = new SimpleTransactionStatus();
    private final TransactionOperations operations = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(status);
        }
    };
    private final SpringTransaccionAdapter adapter = new SpringTransaccionAdapter(operations);

    @Test
    void aSuccessfulResultLeavesTheTransactionToCommit() {
        var result = adapter.<String>ejecutar(() -> Result.success("ok"));

        assertThat(result.fold(value -> value, ApplicationError::code)).isEqualTo("ok");
        assertThat(status.isRollbackOnly()).isFalse();
    }

    @Test
    void aFailureResultMarksTheTransactionForRollbackAndIsReturnedUntouched() {
        var result = adapter.<String>ejecutar(() -> Result.failure(CONFLICTO));

        ApplicationError error = result.fold(value -> null, failure -> failure);
        assertThat(error).isSameAs(CONFLICTO);
        assertThat(status.isRollbackOnly()).isTrue();
    }

    @Test
    void requiresTheTransactionOperations() {
        assertThatNullPointerException().isThrownBy(() -> new SpringTransaccionAdapter(null));
    }
}
