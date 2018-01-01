package io.mcarle.strix;

import io.mcarle.strix.annotation.NoTransaction;
import io.mcarle.strix.annotation.Transactional;

/**
 * Manager, in which some none public methods are annotated with {@link Transactional}, but not the class itself.
 */
public class MethodsAnnotatedWithTransactionalManager {

    @Transactional(persistenceUnit = "strix-pu")
    protected long count_STRIX_PU() {
        return Strix.em().createQuery("SELECT count(*) FROM TestEntity", Long.class).getSingleResult();
    }

    @NoTransaction
    @Transactional(persistenceUnit = "strix-pu")
    protected long count_STRIX_PU_NoTransaction() {
        return Strix.em().createQuery("SELECT count(*) FROM TestEntity", Long.class).getSingleResult();
    }

    public long callCount_STRIX_PU() {
        return count_STRIX_PU();
    }
}
