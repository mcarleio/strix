package io.mcarle.strix.annotation;

import io.mcarle.strix.Strix;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method to be transactional, so it can access an {@link javax.persistence.EntityManager} from
 * {@link Strix#em()}.
 * <p>
 * If annotated on a class, all public methods without {@link NoTransaction} and {@link Transactional} will relate to
 * the options defined in that annotation. If annotated on a method it will override the {@link Transactional}
 * annotation of the class.
 */
@Target(value = {ElementType.METHOD, ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Transactional {

    /**
     * Defines the persistence unit, i.e. which database connection should be used. If not specified and there only
     * exists one persistence unit, strix will use that unit by default.
     *
     * @return Name of the persistence unit
     */
    String persistenceUnit() default "";

    /**
     * Prevent the method from doing updates to the database, i.e. only read allowed
     *
     * @return {@code true}, if the transaction should only be readable and not writeable. Otherwise {@code false}.
     */
    boolean readOnly() default false;

    /**
     * Defines, that the method will create and use its own transaction, i.e. it will create a new transaction whenever
     * the method is called.
     *
     * @return {@code true}, if a new transaction is required. Otherwise {@code false}
     */
    boolean requiresNew() default false;

    /**
     * Defines the time in milliseconds, after which the {@link javax.persistence.EntityTransaction} will be rollbacked
     * and the {@link javax.persistence.EntityManager} will be closed.
     *
     * @return Amount of time in milliseconds
     */
    int timeout() default 0;

    /**
     * Defines expected exceptions, for which strix will not do a rollback
     *
     * @return List of throwable classes, for which no rollback shall be performed
     */
    Class<? extends Throwable>[] noRollbackFor() default {};
}
