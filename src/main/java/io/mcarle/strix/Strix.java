package io.mcarle.strix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Map;

/**
 * Provides methods to start and stop strix, as well as getting the {@link EntityManager}.
 */
public final class Strix {

    private static final Logger LOG = LoggerFactory.getLogger(Strix.class);

    /**
     * Starts strix with default settings
     */
    public static void startup() {
        startup(null, null);
    }

    /**
     * Starts strix with additional properties, which will override the properties of the persistence unit from the
     * persistence.xml
     *
     * @param persistenceProperties Map of persistence unit names to a map of persistence properties.
     */
    public static void startup(Map<String, Map<String, String>> persistenceProperties) {
        startup(persistenceProperties, null);
    }

    /**
     * Starts strix with a default persistence unit, which will be used in all methods annotated with
     * {@link io.mcarle.strix.annotation.Transactional}, when that annotation defines no persistence unit.
     *
     * @param defaultPersistenceUnit The default persistence unit which should be used, when no other unit is specified
     */
    public static void startup(String defaultPersistenceUnit) {
        startup(null, defaultPersistenceUnit);
    }

    /**
     * Starts strix. Combines {@link #startup(Map)} and {@link #startup(String)}.
     *
     * @param persistenceProperties  see {@link #startup(Map)}
     * @param defaultPersistenceUnit see {@link #startup(String)}
     */
    public static void startup(
          Map<String, Map<String, String>> persistenceProperties,
          String defaultPersistenceUnit
    ) {
        LOG.info(
              "Starts strix with default persistence unit '{}' and custom persistence properties '{}'",
              defaultPersistenceUnit,
              persistenceProperties
        );
        StrixManager.startup(persistenceProperties, defaultPersistenceUnit);
    }

    /**
     * Shutdown strix and closes any open {@link javax.persistence.EntityManagerFactory}
     */
    public static void shutdown() {
        LOG.info("Shutdown strix");
        StrixManager.shutdown();
    }

    /**
     * Get the {@link EntityManager} for the current thread.
     *
     * @return If a transaction is active, returns the current {@link EntityManager}, otherwise {@code null}
     */
    public static EntityManager em() {
        LOG.trace("Get EntityManager");
        return PersistenceManager.getEntityManager();
    }
}
