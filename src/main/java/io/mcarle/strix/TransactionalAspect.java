package io.mcarle.strix;

import io.mcarle.strix.annotation.Transactional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class TransactionalAspect {

    /**
     * Checks, if strix is started
     *
     * @return The state of strix: {@code true} if it is started, {@code false} otherwise.
     */
    @Pointcut("if()")
    public static boolean isPersistenceStarted() {
        return StrixManager.STARTED;
    }

    /**
     * Matches any execution of a public method
     */
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    /**
     * Matches any execution of a method
     */
    @Pointcut("execution(* *(..))")
    public void anyMethod() {
    }

    /**
     * Matches any method, which is annotated with {@link io.mcarle.strix.annotation.NoTransaction}
     */
    @Pointcut("@annotation(io.mcarle.strix.annotation.NoTransaction)")
    public void noTransactionAnnotated() {
    }

    /**
     * Matches any method, which is annotated with {@link Transactional}
     */
    @Pointcut("@annotation(io.mcarle.strix.annotation.Transactional)")
    public void transactionalAnnotated() {
    }

    /**
     * If strix is started: Matches any method, which is not annotated with
     * {@link io.mcarle.strix.annotation.NoTransaction}
     */
    @Pointcut("isPersistenceStarted() && anyMethod() && ! noTransactionAnnotated()")
    public void startedAndMethodNotAnnotatedWithNoTransaction() {
    }

    /**
     * If strix is started: Matches any public method, which is not annotated with
     * {@link io.mcarle.strix.annotation.NoTransaction}
     */
    @Pointcut("startedAndMethodNotAnnotatedWithNoTransaction() && publicMethod()")
    public void startedAndPublicMethodNotAnnotatedWithNoTransaction() {
    }

    /**
     * If strix is started: Executes around any public method, which is annotated with {@link Transactional} and not
     * with {@link io.mcarle.strix.annotation.NoTransaction}.
     *
     * @param joinPoint     The join point of AspectJ
     * @param transactional The {@link Transactional} annotation of the method
     * @return The result of the aspected method
     * @throws Throwable If the aspected method throws an exception
     */
    @Around("startedAndMethodNotAnnotatedWithNoTransaction() && @annotation(transactional)")
    public Object aroundMethodAnnotatedWithTransactional(
          ProceedingJoinPoint joinPoint,
          Transactional transactional
    ) throws Throwable {
        return StrixManager.handleTransactionalMethodExecution(joinPoint, transactional);
    }

    /**
     * If strix is started: Executes around any public method, which is not annotated with {@link Transactional} or
     * {@link io.mcarle.strix.annotation.NoTransaction}, but is part of a class annotated with {@link Transactional}.
     *
     * @param joinPoint     The join point of AspectJ
     * @param transactional The {@link Transactional} annotation of the class
     * @return The result of the aspected method
     * @throws Throwable If the aspected method throws an exception
     */
    @Around("startedAndPublicMethodNotAnnotatedWithNoTransaction() && @within(transactional) && ! transactionalAnnotated()")
    public Object aroundMethodNotAnnotatedWithTransactionalInClassAnnotatedWithTransactional(
          ProceedingJoinPoint joinPoint,
          Transactional transactional
    ) throws Throwable {
        return StrixManager.handleTransactionalMethodExecution(joinPoint, transactional);
    }

}
