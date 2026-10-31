package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.CONFLICTO;
import static org.assertj.core.api.Assertions.assertThat;

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
        var result = adapter.ejecutar(() -> Result.success("ok"));

        String texto = result.fold(value -> value, error -> "error");
        assertThat(texto).isEqualTo("ok");
        assertThat(status.isRollbackOnly()).isFalse();
    }

    @Test
    void aFailureResultMarksTheTransactionForRollbackAndIsReturnedUntouched() {
        var result = adapter.<String>ejecutar(() -> Result.failure(CONFLICTO));

        assertThat(result.isFailure()).isTrue();
        assertThat(status.isRollbackOnly()).isTrue();
    }
}
