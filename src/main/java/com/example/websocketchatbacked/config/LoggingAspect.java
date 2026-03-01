package com.example.websocketchatbacked.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.concurrent.Executor;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private static final String REQUEST_ID = "requestId";
    private static final String START_TIME = "startTime";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "token", "password", "secret", "api-key"
    );
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "token", "secret", "apiKey", "authorization"
    );

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${logging.aspect.enabled:true}")
    private boolean loggingEnabled;

    @Autowired
    @Qualifier("loggingExecutor")
    private Executor loggingExecutor;

    @Pointcut("execution(* com.example.websocketchatbacked.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Before("controllerPointcut()")
    public void logBefore(ProceedingJoinPoint joinPoint) {
        if (!loggingEnabled) {
            return;
        }

        loggingExecutor.execute(() -> {
            try {
                HttpServletRequest request = getRequest();
                if (request == null) {
                    return;
                }

                String requestId = generateRequestId();
                MDC.put(REQUEST_ID, requestId);
                MDC.put(START_TIME, String.valueOf(System.currentTimeMillis()));

                Map<String, Object> logData = new LinkedHashMap<>();
                logData.put("requestId", requestId);
                logData.put("timestamp", new Date());
                logData.put("type", "REQUEST");
                logData.put("controller", getControllerName(joinPoint));
                logData.put("method", getMethodName(joinPoint));
                logData.put("httpMethod", request.getMethod());
                logData.put("path", request.getRequestURI());
                logData.put("queryString", request.getQueryString());
                logData.put("clientIp", getClientIp(request));
                logData.put("headers", filterSensitiveHeaders(getHeaders(request)));
                logData.put("parameters", filterSensitiveParams(getParameters(request, joinPoint)));

                logger.info("Request: {}", objectMapper.writeValueAsString(logData));
            } catch (Exception e) {
                logger.error("Error logging request: {}", e.getMessage());
            }
        });
    }

    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void logAfterReturning(ProceedingJoinPoint joinPoint, Object result) {
        if (!loggingEnabled) {
            return;
        }

        loggingExecutor.execute(() -> {
            try {
                String requestId = MDC.get(REQUEST_ID);
                String startTimeStr = MDC.get(START_TIME);

                if (requestId == null || startTimeStr == null) {
                    return;
                }

                long startTime = Long.parseLong(startTimeStr);
                long duration = System.currentTimeMillis() - startTime;

                HttpServletRequest request = getRequest();
                Map<String, Object> logData = new LinkedHashMap<>();
                logData.put("requestId", requestId);
                logData.put("timestamp", new Date());
                logData.put("type", "RESPONSE");
                logData.put("controller", getControllerName(joinPoint));
                logData.put("method", getMethodName(joinPoint));
                logData.put("httpMethod", request != null ? request.getMethod() : "UNKNOWN");
                logData.put("path", request != null ? request.getRequestURI() : "UNKNOWN");
                logData.put("statusCode", 200);
                logData.put("responseSize", calculateResponseSize(result));
                logData.put("duration", duration + "ms");

                logger.info("Response: {}", objectMapper.writeValueAsString(logData));

                MDC.remove(REQUEST_ID);
                MDC.remove(START_TIME);
            } catch (Exception e) {
                logger.error("Error logging response: {}", e.getMessage());
            }
        });
    }

    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "exception")
    public void logAfterThrowing(ProceedingJoinPoint joinPoint, Throwable exception) {
        if (!loggingEnabled) {
            return;
        }

        loggingExecutor.execute(() -> {
            try {
                String requestId = MDC.get(REQUEST_ID);
                String startTimeStr = MDC.get(START_TIME);

                if (requestId == null || startTimeStr == null) {
                    return;
                }

                long startTime = Long.parseLong(startTimeStr);
                long duration = System.currentTimeMillis() - startTime;

                HttpServletRequest request = getRequest();
                Map<String, Object> logData = new LinkedHashMap<>();
                logData.put("requestId", requestId);
                logData.put("timestamp", new Date());
                logData.put("type", "ERROR");
                logData.put("controller", getControllerName(joinPoint));
                logData.put("method", getMethodName(joinPoint));
                logData.put("httpMethod", request != null ? request.getMethod() : "UNKNOWN");
                logData.put("path", request != null ? request.getRequestURI() : "UNKNOWN");
                logData.put("exception", exception.getClass().getSimpleName());
                logData.put("message", exception.getMessage());
                logData.put("duration", duration + "ms");

                logger.error("Exception: {}", objectMapper.writeValueAsString(logData), exception);

                MDC.remove(REQUEST_ID);
                MDC.remove(START_TIME);
            } catch (Exception e) {
                logger.error("Error logging exception: {}", e.getMessage());
            }
        });
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String getControllerName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("Controller", "");
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getName();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private Map<String, String> filterSensitiveHeaders(Map<String, String> headers) {
        Map<String, String> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (SENSITIVE_HEADERS.contains(key)) {
                filtered.put(entry.getKey(), "******");
            } else {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private Map<String, Object> getParameters(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new LinkedHashMap<>();
        
        if (request != null) {
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                params.put(paramName, request.getParameter(paramName));
            }
        }

        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object argValue = args[i];
                
                if (SENSITIVE_PARAMS.contains(paramName.toLowerCase())) {
                    params.put(paramName, "******");
                } else if (argValue != null && !isSimpleType(argValue.getClass())) {
                    try {
                        String json = objectMapper.writeValueAsString(argValue);
                        if (json.length() > 500) {
                            params.put(paramName, json.substring(0, 500) + "... (truncated)");
                        } else {
                            params.put(paramName, json);
                        }
                    } catch (Exception e) {
                        params.put(paramName, argValue.toString());
                    }
                } else {
                    params.put(paramName, argValue);
                }
            }
        }

        return params;
    }

    private Map<String, Object> filterSensitiveParams(Map<String, Object> params) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (SENSITIVE_PARAMS.contains(key)) {
                filtered.put(entry.getKey(), "******");
            } else {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private int calculateResponseSize(Object result) {
        if (result == null) {
            return 0;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return json.getBytes().length;
        } catch (Exception e) {
            return result.toString().getBytes().length;
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz == Character.class ||
                clazz == Date.class;
    }
}
