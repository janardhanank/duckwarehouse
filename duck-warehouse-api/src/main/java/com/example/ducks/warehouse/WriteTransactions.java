package com.example.ducks.warehouse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.SQLException;
import java.util.function.Supplier;

@Component
public class WriteTransactions {
    private final TransactionTemplate transaction;
    public WriteTransactions(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
    }
    public <T> T run(Supplier<T> operation) {
        return transaction.execute(status -> operation.get());
    }
    public <T> T addWithDeadlockRetry(Supplier<T> operation) {
        for (int attempt = 0; ; attempt++) {
            try {
                return run(operation);
            } catch (RuntimeException ex) {
                // MySQL 1213 guarantees the transaction was rolled back.
                // Never retry connection failures or unknown commit outcomes.
                if (attempt >= 2 || !isDeadlock(ex)) throw ex;
                try {
                    Thread.sleep(10L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }
    private boolean isDeadlock(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql && sql.getErrorCode() == 1213) return true;
        }
        return false;
    }
}
