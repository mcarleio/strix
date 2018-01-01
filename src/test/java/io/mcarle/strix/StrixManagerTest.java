package io.mcarle.strix;

import io.mcarle.strix.entity.TestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.PersistenceException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StrixManagerTest {

    private final TransactionalAnnotatedManager transactionalAnnotatedManager = new TransactionalAnnotatedManager();
    private final MethodsAnnotatedWithTransactionalManager methodsAnnotatedWithTransactionalManager = new MethodsAnnotatedWithTransactionalManager();

    @Before
    public void startupPersistence() {
        Strix.startup();
    }

    @After
    public void shutdownPersistence() {
        Strix.shutdown();
    }

    @Test
    public void checkPointcutIsPersistenceStartedTest() {
        StrixManager.STARTED = false;
        assertNull(transactionalAnnotatedManager.getEntityManagerPUBLIC());

        StrixManager.STARTED = true;
        assertNotNull(transactionalAnnotatedManager.getEntityManagerPUBLIC());

        StrixManager.STARTED = false;
        assertNull(transactionalAnnotatedManager.getEntityManagerPUBLIC());
    }

    @Test
    public void checkPointcutPublicMethodTest() {
        assertNotNull(transactionalAnnotatedManager.getEntityManagerPUBLIC());
        assertNull(transactionalAnnotatedManager.getEntityManagerPROTECTED());
        assertNull(transactionalAnnotatedManager.getEntityManagerDEFAULT());
    }

    @Test
    public void checkPointcutNotNoTransactionAnnotatedTest() {
        assertNotNull(transactionalAnnotatedManager.getEntityManagerPUBLIC());
        assertNull(transactionalAnnotatedManager.getEntityManagerNO_TRANSACTION());
    }

    @Test
    public void checkClosedAfterExecutionTest() {
        assertFalse(transactionalAnnotatedManager.getEntityManagerPUBLIC().isOpen());
    }

    @Test
    public void commitTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();

        assertNotNull(transactionalAnnotatedManager.save_STRIX_PU(new TestEntity()).getId());
        assertEquals(countBefore + 1, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test
    public void commitMultipleTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();

        transactionalAnnotatedManager.multisave_STRIX_PU();

        assertEquals(countBefore + 3, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test(expected = RuntimeException.class)
    public void rollbackTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        try {
            transactionalAnnotatedManager.saveMultipleTimesThenException_STRIX_PU();
        } finally {
            assertEquals(countBefore, transactionalAnnotatedManager.count_STRIX_PU());
        }
    }

    @Test(expected = Throwable.class)
    public void rollbackWithThrowableTest() throws Throwable {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        try {
            transactionalAnnotatedManager.saveMultipleTimesThenThrowableInNewTransaction_STRIX_PU();
        } finally {
            assertEquals(countBefore, transactionalAnnotatedManager.count_STRIX_PU());
        }
    }

    @Test(expected = RuntimeException.class)
    public void throwInNewTransaction() throws Throwable {
        transactionalAnnotatedManager.callAnotherMethodInNewTransaction();
    }

    @Test
    public void readOnlyTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        transactionalAnnotatedManager.saveMultipleTimesReadOnly_STRIX_PU();
        assertEquals(countBefore, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test(expected = RuntimeException.class)
    public void noRollbackForTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        try {
            transactionalAnnotatedManager.saveMultipleTimesThenExceptionButNoRollback_STRIX_PU();
        } finally {
            assertEquals(countBefore + 3, transactionalAnnotatedManager.count_STRIX_PU());
        }
    }

    @Test
    public void requiresNewTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        long countInNewTransaction = transactionalAnnotatedManager.saveAndMultisaveAndCountInNewTransaction_STRIX_PU();
        long countAfter = transactionalAnnotatedManager.count_STRIX_PU();
        assertEquals(countBefore + 3, countInNewTransaction);
        assertEquals(countBefore + 4, countAfter);
    }

    @Test(expected = IllegalStateException.class)
    public void timeoutTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        try {
            transactionalAnnotatedManager.executeLongRunning_STRIX_PU(100);
        } finally {
            assertEquals(countBefore, transactionalAnnotatedManager.count_STRIX_PU());
        }
    }

    @Test
    public void persistenceUnitTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        long countOtherPUBefore = transactionalAnnotatedManager.count_STRIX_SECOND_PU();
        try {
            transactionalAnnotatedManager.multisave_STRIX_SECOND_PU();
        } finally {
            assertEquals(countBefore, transactionalAnnotatedManager.count_STRIX_PU());
            assertEquals(countOtherPUBefore + 3, transactionalAnnotatedManager.count_STRIX_SECOND_PU());
        }
    }

    @Test
    public void requiresNewWithExceptionTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        transactionalAnnotatedManager.saveThenThrowInOtherTransactionThenSave_STRIX_PU();
        assertEquals(countBefore + 6, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test
    public void timeoutNotNeededTest() {
        long countBefore = transactionalAnnotatedManager.count_STRIX_PU();
        transactionalAnnotatedManager.executeLongRunning_STRIX_PU(1);
        assertEquals(countBefore + 6, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test(expected = PersistenceException.class)
    public void throwErrorWhenMultiplePersistenceUnitsAndDefaultIsNull() {
        Strix.startup();

        // Should result in PersistenceException, as strix is initialized with no default persistence unit and there
        // is existing more than one in the persistence.xml
        transactionalAnnotatedManager.count_DEFAULT_PU();
    }

    @Test
    public void changeDefaultPersistenceUnit() {
        Map<String, Map<String, String>> persistenceProperties = new HashMap<>();
        persistenceProperties.put("strix-pu", new HashMap<>());
        persistenceProperties.get("strix-pu").put("javax.persistence.jdbc.url", "jdbc:h2:file:./target/junit-h2/strix1");
        persistenceProperties.get("strix-pu").put("javax.persistence.schema-generation.database.action", "create");
        persistenceProperties.put("strix-second-pu", new HashMap<>());
        persistenceProperties.get("strix-second-pu").put("javax.persistence.jdbc.url", "jdbc:h2:file:./target/junit-h2/strix2");
        persistenceProperties.get("strix-second-pu").put("javax.persistence.schema-generation.database.action", "create");

        // Start strix with default-pu = strix-pu
        Strix.startup(persistenceProperties, "strix-pu");

        // Saves some entries in strix-pu
        transactionalAnnotatedManager.multisave_STRIX_PU();

        assertEquals(transactionalAnnotatedManager.count_STRIX_PU(), transactionalAnnotatedManager.count_DEFAULT_PU()); // strix-pu == strix-pu
        assertNotEquals(transactionalAnnotatedManager.count_STRIX_SECOND_PU(), transactionalAnnotatedManager.count_DEFAULT_PU()); // strix-pu != strix-second-pu

        // (Re-)Start strix with different default-pu
        Strix.startup(persistenceProperties, "strix-second-pu");

        assertNotEquals(transactionalAnnotatedManager.count_STRIX_PU(), transactionalAnnotatedManager.count_DEFAULT_PU()); // strix-pu != strix-second-pu
        assertEquals(transactionalAnnotatedManager.count_STRIX_SECOND_PU(), transactionalAnnotatedManager.count_DEFAULT_PU()); // strix-second-pu == strix-second-pu

        // (Re-)Start strix with strix-pu and default persistence properties
        Strix.startup("strix-pu");

        assertEquals(0, transactionalAnnotatedManager.count_STRIX_PU());
        assertEquals(0, transactionalAnnotatedManager.count_DEFAULT_PU());
        assertEquals(0, transactionalAnnotatedManager.count_STRIX_SECOND_PU());
    }

    @Test(expected = PersistenceException.class)
    public void settingDifferentPersistenceProperties() {
        Map<String, Map<String, String>> persistenceProperties = new HashMap<>();
        persistenceProperties.put("strix-pu", new HashMap<>());
        persistenceProperties.get("strix-pu").put("javax.persistence.jdbc.url", "jdbc:h2:mem:changedstrix;DB_CLOSE_DELAY=-1");
        persistenceProperties.get("strix-pu").put("javax.persistence.schema-generation.database.action", "none");
        Strix.startup(persistenceProperties);

        // Should result in an PersistenceException, because the table was not created
        transactionalAnnotatedManager.count_STRIX_PU();
    }

    @Test
    public void assertMixOfDifferentPU() {
        transactionalAnnotatedManager.saveInCurrentAndSaveInDifferentPU_STRIX_PU();
        assertEquals(3, transactionalAnnotatedManager.count_STRIX_SECOND_PU());
        assertEquals(1, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test
    public void rollback() {
        transactionalAnnotatedManager.rollback_STRIX_PU();
        assertEquals(0, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test
    public void timeout() {
        transactionalAnnotatedManager.timeout_STRIX_PU();
        assertEquals(0, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test(expected = PersistenceException.class)
    public void jtaManaged() {
        transactionalAnnotatedManager.jtaManaged_STRIX_THIRD_PU();
    }

    @Test
    public void differentExceptionThanExpected() {
        transactionalAnnotatedManager.multisave_STRIX_PU();
        assertEquals(3, transactionalAnnotatedManager.count_STRIX_PU());
        try {
            transactionalAnnotatedManager.throwUnexceptionException_STRIX_PU();
        } catch (NullPointerException npe) {
        }
        assertEquals(3, transactionalAnnotatedManager.count_STRIX_PU());
    }

    @Test
    public void specialTimeoutCaseClose() {
        transactionalAnnotatedManager.specialTimeoutCaseClose_STRIX_PU();
    }

    @Test
    public void specialTimeoutCaseRollback() {
        transactionalAnnotatedManager.specialTimeoutCaseRollback_STRIX_PU();
    }

    @Test
    public void callMethodsForCompleteness() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        new StrixManager();
        new Strix();
        new PersistenceManager();
        TransactionalAspect.class.getMethod("aspectOf").invoke(null);
        TransactionalAspect.class.getMethod("hasAspect").invoke(null);
    }

    @Test
    public void methodAnnotatedWithTransactional() {
        assertEquals(0, methodsAnnotatedWithTransactionalManager.count_STRIX_PU());
    }

    @Test(expected = NullPointerException.class)
    public void methodAnnotatedWithTransactionalAndNoTransaction() {
        methodsAnnotatedWithTransactionalManager.count_STRIX_PU_NoTransaction();
    }

    @Test
    public void publicMethodNotAnnotatedWithTransactionalButInternalCallingTransactionalAnnotatedMethod() {
        assertEquals(0, methodsAnnotatedWithTransactionalManager.callCount_STRIX_PU());
    }


}
