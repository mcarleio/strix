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

    private final TestManager testManager = new TestManager();

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
        assertNull(testManager.getEntityManagerPUBLIC());

        StrixManager.STARTED = true;
        assertNotNull(testManager.getEntityManagerPUBLIC());

        StrixManager.STARTED = false;
        assertNull(testManager.getEntityManagerPUBLIC());
    }

    @Test
    public void checkPointcutPublicMethodTest() {
        assertNotNull(testManager.getEntityManagerPUBLIC());
        assertNull(testManager.getEntityManagerPROTECTED());
        assertNull(testManager.getEntityManagerDEFAULT());
    }

    @Test
    public void checkPointcutNotNoTransactionAnnotatedTest() {
        assertNotNull(testManager.getEntityManagerPUBLIC());
        assertNull(testManager.getEntityManagerNO_TRANSACTION());
    }

    @Test
    public void checkClosedAfterExecutionTest() {
        assertFalse(testManager.getEntityManagerPUBLIC().isOpen());
    }

    @Test
    public void commitTest() {
        long countBefore = testManager.count_STRIX_PU();

        assertNotNull(testManager.save_STRIX_PU(new TestEntity()).getId());
        assertEquals(countBefore + 1, testManager.count_STRIX_PU());
    }

    @Test
    public void commitMultipleTest() {
        long countBefore = testManager.count_STRIX_PU();

        testManager.multisave_STRIX_PU();

        assertEquals(countBefore + 3, testManager.count_STRIX_PU());
    }

    @Test(expected = RuntimeException.class)
    public void rollbackTest() {
        long countBefore = testManager.count_STRIX_PU();
        try {
            testManager.saveMultipleTimesThenException_STRIX_PU();
        } finally {
            assertEquals(countBefore, testManager.count_STRIX_PU());
        }
    }

    @Test(expected = Throwable.class)
    public void rollbackWithThrowableTest() throws Throwable {
        long countBefore = testManager.count_STRIX_PU();
        try {
            testManager.saveMultipleTimesThenThrowableInNewTransaction_STRIX_PU();
        } finally {
            assertEquals(countBefore, testManager.count_STRIX_PU());
        }
    }

    @Test(expected = RuntimeException.class)
    public void throwInNewTransaction() throws Throwable {
        testManager.callAnotherMethodInNewTransaction();
    }

    @Test
    public void readOnlyTest() {
        long countBefore = testManager.count_STRIX_PU();
        testManager.saveMultipleTimesReadOnly_STRIX_PU();
        assertEquals(countBefore, testManager.count_STRIX_PU());
    }

    @Test(expected = RuntimeException.class)
    public void noRollbackForTest() {
        long countBefore = testManager.count_STRIX_PU();
        try {
            testManager.saveMultipleTimesThenExceptionButNoRollback_STRIX_PU();
        } finally {
            assertEquals(countBefore + 3, testManager.count_STRIX_PU());
        }
    }

    @Test
    public void requiresNewTest() {
        long countBefore = testManager.count_STRIX_PU();
        long countInNewTransaction = testManager.saveAndMultisaveAndCountInNewTransaction_STRIX_PU();
        long countAfter = testManager.count_STRIX_PU();
        assertEquals(countBefore + 3, countInNewTransaction);
        assertEquals(countBefore + 4, countAfter);
    }

    @Test(expected = IllegalStateException.class)
    public void timeoutTest() {
        long countBefore = testManager.count_STRIX_PU();
        try {
            testManager.executeLongRunning_STRIX_PU(100);
        } finally {
            assertEquals(countBefore, testManager.count_STRIX_PU());
        }
    }

    @Test
    public void persistenceUnitTest() {
        long countBefore = testManager.count_STRIX_PU();
        long countOtherPUBefore = testManager.count_STRIX_SECOND_PU();
        try {
            testManager.multisave_STRIX_SECOND_PU();
        } finally {
            assertEquals(countBefore, testManager.count_STRIX_PU());
            assertEquals(countOtherPUBefore + 3, testManager.count_STRIX_SECOND_PU());
        }
    }

    @Test
    public void requiresNewWithExceptionTest() {
        long countBefore = testManager.count_STRIX_PU();
        testManager.saveThenThrowInOtherTransactionThenSave_STRIX_PU();
        assertEquals(countBefore + 6, testManager.count_STRIX_PU());
    }

    @Test
    public void timeoutNotNeededTest() {
        long countBefore = testManager.count_STRIX_PU();
        testManager.executeLongRunning_STRIX_PU(1);
        assertEquals(countBefore + 6, testManager.count_STRIX_PU());
    }

    @Test(expected = PersistenceException.class)
    public void throwErrorWhenMultiplePersistenceUnitsAndDefaultIsNull() {
        Strix.startup();

        // Should result in PersistenceException, as strix is initialized with no default persistence unit and there
        // is existing more than one in the persistence.xml
        testManager.count_DEFAULT_PU();
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
        testManager.multisave_STRIX_PU();

        assertEquals(testManager.count_STRIX_PU(), testManager.count_DEFAULT_PU()); // strix-pu == strix-pu
        assertNotEquals(testManager.count_STRIX_SECOND_PU(), testManager.count_DEFAULT_PU()); // strix-pu != strix-second-pu

        // (Re-)Start strix with different default-pu
        Strix.startup(persistenceProperties, "strix-second-pu");

        assertNotEquals(testManager.count_STRIX_PU(), testManager.count_DEFAULT_PU()); // strix-pu != strix-second-pu
        assertEquals(testManager.count_STRIX_SECOND_PU(), testManager.count_DEFAULT_PU()); // strix-second-pu == strix-second-pu

        // (Re-)Start strix with strix-pu and default persistence properties
        Strix.startup("strix-pu");

        assertEquals(0, testManager.count_STRIX_PU());
        assertEquals(0, testManager.count_DEFAULT_PU());
        assertEquals(0, testManager.count_STRIX_SECOND_PU());
    }

    @Test(expected = PersistenceException.class)
    public void settingDifferentPersistenceProperties() {
        Map<String, Map<String, String>> persistenceProperties = new HashMap<>();
        persistenceProperties.put("strix-pu", new HashMap<>());
        persistenceProperties.get("strix-pu").put("javax.persistence.jdbc.url", "jdbc:h2:mem:changedstrix;DB_CLOSE_DELAY=-1");
        persistenceProperties.get("strix-pu").put("javax.persistence.schema-generation.database.action", "none");
        Strix.startup(persistenceProperties);

        // Should result in an PersistenceException, because the table was not created
        testManager.count_STRIX_PU();
    }

    @Test
    public void assertMixOfDifferentPU() {
        testManager.saveInCurrentAndSaveInDifferentPU_STRIX_PU();
        assertEquals(3, testManager.count_STRIX_SECOND_PU());
        assertEquals(1, testManager.count_STRIX_PU());
    }

    @Test
    public void rollback() {
        testManager.rollback_STRIX_PU();
        assertEquals(0, testManager.count_STRIX_PU());
    }

    @Test
    public void timeout() {
        testManager.timeout_STRIX_PU();
        assertEquals(0, testManager.count_STRIX_PU());
    }

    @Test(expected = PersistenceException.class)
    public void jtaManaged() {
        testManager.jtaManaged_STRIX_THIRD_PU();
    }

    @Test
    public void differentExceptionThanExpected() {
        testManager.multisave_STRIX_PU();
        assertEquals(3, testManager.count_STRIX_PU());
        try {
            testManager.throwUnexceptionException_STRIX_PU();
        } catch (NullPointerException npe) {
        }
        assertEquals(3, testManager.count_STRIX_PU());
    }

    @Test
    public void specialTimeoutCaseClose() {
        testManager.specialTimeoutCaseClose_STRIX_PU();
    }

    @Test
    public void specialTimeoutCaseRollback() {
        testManager.specialTimeoutCaseRollback_STRIX_PU();
    }

    @Test
    public void callMethodsForCompleteness() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        new StrixManager();
        new Strix();
        new PersistenceManager();
        TransactionalAspect.class.getMethod("aspectOf").invoke(null);
        TransactionalAspect.class.getMethod("hasAspect").invoke(null);
    }

}
