package com.acmeair.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Simple dependency injection container to replace ServiceLocator
 */
public class ServiceRegistry {

    private static final Logger logger = Logger.getLogger(ServiceRegistry.class.getName());
    private static ServiceRegistry instance;
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();

    private ServiceRegistry() {
        // Register default implementations for Morphia
        registerImplementation(CustomerService.class, "com.acmeair.morphia.services.CustomerServiceImpl");
        registerImplementation(FlightService.class, "com.acmeair.morphia.services.FlightServiceImpl");
        registerImplementation(BookingService.class, "com.acmeair.morphia.services.BookingServiceImpl");
        registerImplementation(TransactionService.class, "com.acmeair.morphia.services.TransactionServiceImpl");
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry();
        }
        return instance;
    }

    private void registerImplementation(Class<?> serviceInterface, String implementationClassName) {
        try {
            Class<?> implClass = Class.forName(implementationClassName);
            implementations.put(serviceInterface, implClass);
            logger.info("Registered implementation: " + implementationClassName + " for " + serviceInterface.getSimpleName());
        } catch (ClassNotFoundException e) {
            logger.warning("Implementation not found: " + implementationClassName);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceInterface) {
        T service = (T) services.get(serviceInterface);
        if (service == null) {
            service = createService(serviceInterface);
            if (service != null) {
                services.put(serviceInterface, service);
            }
        }
        return service;
    }

    @SuppressWarnings("unchecked")
    private <T> T createService(Class<T> serviceInterface) {
        try {
            Class<?> implClass = implementations.get(serviceInterface);
            if (implClass == null) {
                logger.severe("No implementation registered for: " + serviceInterface.getName());
                return null;
            }

            Object instance = implClass.getDeclaredConstructor().newInstance();

            // Call initialization method if it exists
            try {
                java.lang.reflect.Method initMethod = implClass.getMethod("initialization");
                initMethod.invoke(instance);
                logger.info("Called initialization for " + implClass.getSimpleName());
            } catch (NoSuchMethodException e) {
                // No initialization method, that's fine
            }

            return (T) instance;
        } catch (Exception e) {
            logger.severe("Failed to create service " + serviceInterface.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Register a service instance directly (useful for testing)
     */
    public <T> void registerService(Class<T> serviceInterface, T serviceInstance) {
        services.put(serviceInterface, serviceInstance);
    }

    /**
     * Clear all services (useful for testing)
     */
    public void clear() {
        services.clear();
    }
}