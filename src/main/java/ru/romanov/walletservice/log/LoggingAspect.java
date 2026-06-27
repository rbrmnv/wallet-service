package ru.romanov.walletservice.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* ru.romanov.walletservice.service..*(..))")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object serviceLogger(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodShortName = joinPoint.getSignature().toShortString();

        log.info(String.format("Calling %s with args %s", methodShortName,
                Arrays.toString(joinPoint.getArgs())));

        try {
            Object result = joinPoint.proceed();

            log.info(String.format("Method %s completed", methodShortName));

            return result;
        } catch (Exception exception) {

            log.warn(String.format("Method %s threw %s: %s", methodShortName,
                    exception.getClass().getSimpleName(), exception.getMessage()));

            throw exception;
        }
    }
}
