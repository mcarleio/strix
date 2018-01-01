package io.mcarle.strix;

import io.mcarle.strix.annotation.NoTransaction;
import io.mcarle.strix.annotation.Transactional;
import io.mcarle.strix.entity.TestEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import static org.junit.Assert.fail;

/**
 * Manager, in which every not {@link Transactional} annotated public method should work with the persistence unit
 * {@code strix-pu}.
 */
@Transactional(persistenceUnit = "strix-pu")
public class TransactionalAnnotatedManager {

    /* ======================= ==== ======================= */
    /* =======================  EM  ======================= */
    /* ======================= ==== ======================= */

    public EntityManager getEntityManagerPUBLIC() {
        return Strix.em();
    }

    EntityManager getEntityManagerDEFAULT() {
        return Strix.em();
    }

    protected EntityManager getEntityManagerPROTECTED() {
        return Strix.em();
    }

    @NoTransaction
    public EntityManager getEntityManagerNO_TRANSACTION() {
        return Strix.em();
    }

    public TestEntity save_STRIX_PU(TestEntity testEntity) {
        return Strix.em().merge(testEntity);
    }

    /* ======================= ======= ======================= */
    /* =======================  COUNT  ======================= */
    /* ======================= ======= ======================= */

    private long count() {
        return Strix.em().createQuery("SELECT count(*) FROM TestEntity", Long.class).getSingleResult();
    }

    @Transactional(persistenceUnit = "strix-pu")
    public long count_STRIX_PU() {
        return count();
    }

    @Transactional(persistenceUnit = "strix-second-pu")
    public long count_STRIX_SECOND_PU() {
        return count();
    }

    @Transactional
    public long count_DEFAULT_PU() {
        return count();
    }

    /* ======================= ======= ======================= */
    /* =======================  SAVE  ======================= */
    /* ======================= ======= ======================= */

    private TestEntity save() {
        return Strix.em().merge(new TestEntity());
    }

    @Transactional(persistenceUnit = "strix-pu")
    public TestEntity save_STRIX_PU() {
        return save();
    }

    /* ======================= ========== ======================= */
    /* =======================  MULTISAVE ======================= */
    /* ======================= ========== ======================= */

    private void multisave() {
        save();
        save();
        save();
    }

    @Transactional(persistenceUnit = "strix-pu")
    public void multisave_STRIX_PU() {
        multisave();
    }


    @Transactional(persistenceUnit = "strix-second-pu")
    public void multisave_STRIX_SECOND_PU() {
        multisave();
    }

    @Transactional(readOnly = true, persistenceUnit = "strix-pu")
    public void saveMultipleTimesReadOnly_STRIX_PU() {
        multisave();
    }

    public void saveMultipleTimesThenException_STRIX_PU() {
        multisave();
        throw new RuntimeException();
    }

    public void saveMultipleTimesThenThrowableInNewTransaction_STRIX_PU() throws Throwable {
        multisave();
        saveMultipleTimesThenRollbackWithThrowable_STRIX_PU();
    }


    @Transactional(requiresNew = true, persistenceUnit = "strix-pu")
    public void saveMultipleTimesThenRollbackWithThrowable_STRIX_PU() throws Throwable {
        multisave();
        throw new Throwable();
    }

    @Transactional(noRollbackFor = RuntimeException.class, persistenceUnit = "strix-pu")
    public void saveMultipleTimesThenExceptionButNoRollback_STRIX_PU() {
        multisave();
        throw new RuntimeException();
    }

    public void callAnotherMethodInNewTransaction() {
        throwInOwnTransaction_STRIX_PU();
    }

    @Transactional(requiresNew = true, persistenceUnit = "strix-pu")
    public void throwInOwnTransaction_STRIX_PU() {
        throw new RuntimeException();
    }

    @Transactional(persistenceUnit = "strix-pu")
    public long saveAndMultisaveAndCountInNewTransaction_STRIX_PU() {
        save();
        return multisaveAndCountInOwnTransaction_STRIX_PU();
    }

    @Transactional(requiresNew = true, persistenceUnit = "strix-pu")
    public long multisaveAndCountInOwnTransaction_STRIX_PU() {
        multisave();
        return count();
    }

    public void saveThenThrowInOtherTransactionThenSave_STRIX_PU() {
        multisave();
        try {
            saveMultipleTimesThenRollbackWithThrowable_STRIX_PU();
            fail();
        } catch (Throwable t) {
            multisave();
        }
    }

    @Transactional(timeout = 70, persistenceUnit = "strix-pu")
    public void executeLongRunning_STRIX_PU(int sleep) {
        multisave();
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ex) {
        }
        multisave();
    }

    public void saveInCurrentAndSaveInDifferentPU_STRIX_PU() {
        save_STRIX_PU();
        multisave_STRIX_SECOND_PU();
    }

    @Transactional(persistenceUnit = "strix-pu", timeout = 500)
    public void rollback_STRIX_PU() {
        save();
        Strix.em().getTransaction().rollback();
    }

    @Transactional(persistenceUnit = "strix-pu", timeout = 500)
    public void timeout_STRIX_PU() {
        save();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    @Transactional(persistenceUnit = "strix-pu", noRollbackFor = PersistenceException.class)
    public void throwUnexceptionException_STRIX_PU() {
        multisave();
        throw new NullPointerException();
    }

    @Transactional(persistenceUnit = "strix-third-pu", timeout = 1000)
    public void jtaManaged_STRIX_THIRD_PU() {
        save();
    }

    public void specialTimeoutCaseClose_STRIX_PU() {
        EntityManager em = Strix.em();
        StrixManager.startTimeoutChecker(
              200,
              em,
              em.getTransaction()
        );
        em.close();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    public void specialTimeoutCaseRollback_STRIX_PU() {
        EntityManager em = Strix.em();
        EntityTransaction transaction = em.getTransaction();
        StrixManager.startTimeoutChecker(
              200,
              em,
              transaction
        );
        transaction.rollback();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }
}
