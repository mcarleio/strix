package io.mcarle.strix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * Internaly used by strix to bind the entity manager and the persistence unit, to which the entity manager belongs, to
 * the transactional thread. After the transactional method is finished, strix unbinds everything.
 */
final class PersistenceManager {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceManager.class);
    private static final ThreadLocal<String> ENTITY_MANAGER_PERSISTENCE_UNIT_STORE = new ThreadLocal<>();
    private static final ThreadLocal<EntityManager> ENTITY_MANAGER_STORE = new ThreadLocal<>();

    /**
     * Bind the used persistence unit and entity manager to the current thread
     *
     * @param persistenceUnit Used persistence unit
     * @param entityManager   Used entity manager
     */
    static void setEntityManager(String persistenceUnit, EntityManager entityManager) {
        LOG.trace("Bind entity manager and persistence unit ({}) to current thread", persistenceUnit);
        ENTITY_MANAGER_PERSISTENCE_UNIT_STORE.set(persistenceUnit);
        ENTITY_MANAGER_STORE.set(entityManager);
    }

    /**
     * Unbinds the entity manager and persistence unit from current thread
     */
    static void clearEntityManager() {
        LOG.trace("Unbind entity manager and persistence unit from current thread");
        ENTITY_MANAGER_PERSISTENCE_UNIT_STORE.remove();
        ENTITY_MANAGER_STORE.remove();
    }

    /**
     * Checks if there is an entity manager bound to current thread
     *
     * @return {@code true}, if an entity manager is bound to the current thread. Otherwise {@code false}.
     */
    static boolean isEntityManagerPresent() {
        return ENTITY_MANAGER_STORE.get() != null;
    }

    /**
     * Checks if there is an entity manager bound to current thread and if it belongs to the delivered persistence unit
     *
     * @param persistenceUnit The persistence unit name to which the entity manager should belong to
     * @return {@code true}, if the entity manager belongs to {@code persistenceUnit}. Otherwise {@code false}.
     */
    static boolean isEntityManagerFromPU(String persistenceUnit) {
        return persistenceUnit.equals(ENTITY_MANAGER_PERSISTENCE_UNIT_STORE.get());
    }

    /**
     * Returns the entity manager bpund to the current thread.
     *
     * @return The entity manager bound to the current thread, or {@code null} if no entity manager is bound.
     */
    static EntityManager getEntityManager() {
        return ENTITY_MANAGER_STORE.get();
    }
}
