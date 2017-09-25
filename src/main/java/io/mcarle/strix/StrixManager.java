package io.mcarle.strix;

import io.mcarle.strix.annotation.Transactional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.touk.throwing.ThrowingFunction;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Strix's main logic.
 */
final class StrixManager {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalAspect.class);
    private static final Map<String, EntityManagerFactory> SESSION_FACTORY_STORE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, String>> PERSISTENCE_PROPERTIES = new ConcurrentHashMap<>();
    private static final String STRIX_DEFAULT_PERSISTENCE_UNIT = "DUMMY_VALUE";
    static boolean STARTED = false;
    private static String DEFAULT_PERSISTENCE_UNIT = STRIX_DEFAULT_PERSISTENCE_UNIT;

    /**
     * Start strix with additional persistence properties and a default persistence unit.
     *
     * @param persistenceProperties  Map of persistence unit to persistence properties map
     * @param defaultPersistenceUnit The default persistence unit
     */
    static void startup(Map<String, Map<String, String>> persistenceProperties, String defaultPersistenceUnit) {
        LOG.trace("Startup strix");
        if (STARTED) {
            LOG.trace("Strix already running, shutdown");
            shutdown();
        }
        LOG.trace("Set default persistence unit to '{}'", defaultPersistenceUnit);
        DEFAULT_PERSISTENCE_UNIT = defaultPersistenceUnit;

        if (persistenceProperties != null) {
            LOG.trace("Save persistence properties");
            persistenceProperties.keySet().forEach(key ->
                  PERSISTENCE_PROPERTIES.put(key, Collections.unmodifiableMap(persistenceProperties.get(key)))
            );
        }
        STARTED = true;
        LOG.info("Strix started");
    }

    /**
     * Shutdown strix, i.e. close all {@link EntityManagerFactory} and clear persistence properties.
     */
    static void shutdown() {
        LOG.trace("Shutdown strix");
        STARTED = false;
        LOG.info("Close all open EntityManagerFactories.");
        SESSION_FACTORY_STORE.values().forEach(EntityManagerFactory::close);
        SESSION_FACTORY_STORE.clear();
        LOG.debug("Restore initial default values");
        DEFAULT_PERSISTENCE_UNIT = STRIX_DEFAULT_PERSISTENCE_UNIT;
        PERSISTENCE_PROPERTIES.clear();
    }

    /**
     * Called whenever from the {@link TransactionalAspect} and ensures that the method runs in a transactional context,
     * i.e. ensures there is an open {@link EntityManager} when invoking {@link Strix#em()}.
     *
     * @param joinPoint     The aspectj reference to the aspected method
     * @param transactional The {@link Transactional} annotation of the aspected method
     * @return The result of the aspected method
     * @throws Throwable If the aspected method throws an exception
     */
    static Object handleTransactionalMethodExecution(
          ProceedingJoinPoint joinPoint,
          Transactional transactional
    ) throws Throwable {
        LOG.trace("Handle @Transactional method execution");
        String persistenceUnit = transactional.persistenceUnit();
        if (!PersistenceManager.isEntityManagerPresent()) {
            LOG.debug("No transaction active in current thread");

            Class<? extends Throwable>[] noRollbackFor = transactional.noRollbackFor();
            boolean readOnly = transactional.readOnly();
            final int timeoutTime = transactional.timeout();

            return executeWithTransaction(
                  (em) -> joinPoint.proceed(),
                  persistenceUnit,
                  timeoutTime,
                  noRollbackFor,
                  readOnly
            );
        } else if (!PersistenceManager.isEntityManagerFromPU(persistenceUnit) || transactional.requiresNew()) {
            LOG.debug(
                  "New EntityManager needed, as requiresNew ({}) or different peristence unit ({}) defined.",
                  transactional.requiresNew(),
                  persistenceUnit
            );
            FutureTask<Object> future = new FutureTask<>(() -> {
                try {
                    return handleTransactionalMethodExecution(joinPoint, transactional);
                } catch (Exception ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw new TransactionalExecutionException(ex);
                }
            });
            try {
                LOG.trace("Start execution in own thread");
                Thread thread = new Thread(future);
                thread.start();
                return future.get(); // Waits, till the thread finishes
            } catch (ExecutionException ee) {
                if (ee.getCause() instanceof TransactionalExecutionException) {
                    throw ee.getCause().getCause();
                } else {
                    throw ee.getCause();
                }
            }
        } else {
            LOG.trace("Already inside a transactional context, proceed method execution");
            return joinPoint.proceed();
        }
    }

    /**
     * Starts a thread which will close the {@code em} after the specified {@code timeoutTime}.
     *
     * @param timeoutTime Time in milliseconds
     * @param em          The {@link EntityManager}, which may be used of the aspected method
     * @param transaction The {@link EntityTransaction}, which will be marked as rollback-only if the timeout is reached
     * @return The started timeout thread
     */
    static Thread startTimeoutChecker(final int timeoutTime, EntityManager em, EntityTransaction transaction) {
        LOG.trace("Starts the timeout thread with {}ms", timeoutTime);
        Thread thread = new Thread(() -> {
            try {
                Thread.currentThread().setName("STRIX-TT");
                Thread.sleep(timeoutTime);
                LOG.trace("Timeout thread reached timeout time ({}ms)", timeoutTime);
                if (em.isOpen()) {
                    if (transaction.isActive()) {
                        LOG.trace("Mark the transaction to rollbackOnly");
                        transaction.setRollbackOnly();
                    }
                    LOG.trace("Close EntityManager");
                    em.close();
                }
            } catch (InterruptedException ex) {
                // Ignore InterruptedException
            }
        });
        thread.start();
        return thread;
    }

    /**
     * Checks if {@code t} is marked as an exception, for which no rollback should be done.
     *
     * @param noRollbackFor List of no-rollback-exceptions
     * @param t             The actual exception of the aspected method
     * @return {@code false}, if {@code t} or a superclass of {@code t} is an exception specified in
     * {@code noRollbackFor}. Otherwise {@code true}.
     */
    private static boolean checkNeedForRollback(Class<? extends Throwable>[] noRollbackFor, Throwable t) {
        for (Class<? extends Throwable> exceptionClass : noRollbackFor) {
            if (exceptionClass.isAssignableFrom(t.getClass())) {
                LOG.trace("Exception {} is expected, i.e. no rollback is needed", t.getClass());
                return false;
            }
        }
        return true;
    }

    /**
     * Executes the aspected method within a session, i.e. opens and closes an {@link EntityManager} before and after
     * execution.
     *
     * @param function        The function, in which the aspected method will be executed
     * @param persistenceUnit The persistence unit to identify the {@link EntityManagerFactory} from which the
     *                        {@link EntityManager} will be created.
     * @return The result of the aspected method
     * @throws Throwable If the aspected method throws an exception
     */
    private static Object executeWithSession(
          ThrowingFunction<EntityManager, Object, Throwable> function,
          String persistenceUnit
    ) throws Throwable {
        LOG.trace("Create new EntityManager from persistence unit {}", persistenceUnit);
        EntityManager em = getEntityManagerFactory(persistenceUnit).createEntityManager();
        try {
            PersistenceManager.setEntityManager(persistenceUnit, em);
            return function.apply(em);
        } finally {
            PersistenceManager.clearEntityManager();
            if (em.isOpen()) {
                LOG.trace("Close EntityManager");
                em.close();
            }
        }
    }

    /**
     * Executes the aspected method within a transaction, i.e. opens and commits or rollbacks an
     * {@link EntityTransaction} before and after execution.
     *
     * @param function        The function, which should be executed
     * @param persistenceUnit The persistence unit to identify the {@link EntityManagerFactory} from which the
     *                        {@link EntityManager} will be created.
     * @param timeoutTime     The specified timeout time
     * @param noRollbackFor   The specified list of exceptions
     * @param readOnly        The specified value for read-only
     * @return The result of the aspected method
     * @throws Throwable If the aspected method throws an exception
     */
    private static Object executeWithTransaction(
          ThrowingFunction<EntityManager, Object, Throwable> function,
          String persistenceUnit,
          int timeoutTime,
          Class<? extends Throwable>[] noRollbackFor,
          boolean readOnly
    ) throws Throwable {
        return executeWithSession((em) -> {
            EntityTransaction transaction = em.getTransaction(); // Will never be invoked on JTA EM
            boolean rollback = false;
            Thread timeoutThread = null;
            try {
                LOG.trace("Start a new transaction");
                transaction.begin();
                if (readOnly) {
                    LOG.trace("Set transaction to be read-only");
                    transaction.setRollbackOnly();
                }
                if (timeoutTime > 0) {
                    timeoutThread = startTimeoutChecker(timeoutTime, em, transaction);
                }
                return function.apply(em);
            } catch (Throwable t) {
                rollback = checkNeedForRollback(noRollbackFor, t);
                throw t;
            } finally {
                if (timeoutThread != null && timeoutThread.isAlive()) {
                    LOG.trace("Interrupt timeout thread");
                    timeoutThread.interrupt();
                }
                if (em.isOpen() && transaction.isActive()) {
                    if (rollback || transaction.getRollbackOnly()) {
                        LOG.trace(
                              "Rollback transaction because of unexpected exception ({}) or marked as read-only ({})",
                              rollback,
                              transaction.getRollbackOnly()
                        );
                        transaction.rollback();
                    } else {
                        LOG.trace("Commit transaction");
                        transaction.commit();
                    }
                }
            }
        }, persistenceUnit);
    }

    /**
     * Opens an {@link EntityManagerFactory} for the provided {@code persistenceUnit} if not already opened/cached.
     *
     * @param persistenceUnit The name of the persistence unit
     * @return The {@link EntityManagerFactory} for {@code persistenceUnit}
     */
    private static synchronized EntityManagerFactory getEntityManagerFactory(String persistenceUnit) {
        if (persistenceUnit.isEmpty() && DEFAULT_PERSISTENCE_UNIT != null) {
            persistenceUnit = DEFAULT_PERSISTENCE_UNIT;
        }
        if (!SESSION_FACTORY_STORE.containsKey(persistenceUnit)) {
            LOG.debug("Create new EntityManagerFactory for persistence unit {}", persistenceUnit);
            SESSION_FACTORY_STORE.put(
                  persistenceUnit,
                  Persistence.createEntityManagerFactory(
                        persistenceUnit.isEmpty() ? null : persistenceUnit,
                        PERSISTENCE_PROPERTIES.get(persistenceUnit)
                  )
            );
        }
        return SESSION_FACTORY_STORE.get(persistenceUnit);
    }

    /**
     * An only internal used exception
     */
    private static class TransactionalExecutionException extends RuntimeException {

        private TransactionalExecutionException(Throwable cause) {
            super(cause);
        }

    }
}
