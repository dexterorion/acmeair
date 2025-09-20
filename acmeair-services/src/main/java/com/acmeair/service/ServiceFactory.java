package com.acmeair.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple factory to replace CDI dependency injection
 */
public class ServiceFactory {

    private static final Logger logger = Logger.getLogger(ServiceFactory.class.getName());
    private static ServiceFactory instance;
    private Map<Class<?>, Object> services = new HashMap<>();
    private String serviceType = "morphia"; // default to morphia

    private ServiceFactory() {
        // Determine service type from environment/properties
        String type = System.getProperty("com.acmeair.repository.type");
        if (type == null) {
            type = System.getenv("com.acmeair.repository.type");
        }
        if (type != null) {
            serviceType = type;
        }
        logger.info("ServiceFactory initialized with service type: " + serviceType);
    }

    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
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
            // For now, we'll hardcode the morphia implementations
            // You can extend this to support other implementations later
            Object instance = null;

            if (serviceInterface == CustomerService.class) {
                Class<?> implClass = Class.forName("com.acmeair.morphia.services.CustomerServiceImpl");
                instance = implClass.getDeclaredConstructor().newInstance();
                // Call @PostConstruct method
                callPostConstruct(instance);
            } else if (serviceInterface == FlightService.class) {
                Class<?> implClass = Class.forName("com.acmeair.morphia.services.FlightServiceImpl");
                instance = implClass.getDeclaredConstructor().newInstance();
                // Call @PostConstruct method
                callPostConstruct(instance);
            } else if (serviceInterface == BookingService.class) {
                Class<?> implClass = Class.forName("com.acmeair.morphia.services.BookingServiceImpl");
                instance = implClass.getDeclaredConstructor().newInstance();
                // Call @PostConstruct method
                callPostConstruct(instance);
            } else if (serviceInterface == TransactionService.class) {
                Class<?> implClass = Class.forName("com.acmeair.morphia.services.TransactionServiceImpl");
                instance = implClass.getDeclaredConstructor().newInstance();
                // No @PostConstruct needed for this service
            }

            if (instance == null) {
                logger.warning("No implementation found for service: " + serviceInterface.getName());
                return null;
            }

            return (T) instance;
        } catch (Exception e) {
            logger.severe("Failed to create service " + serviceInterface.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void callPostConstruct(Object instance) {
        try {
            // Look for method named "initialization" and call it
            java.lang.reflect.Method initMethod = instance.getClass().getMethod("initialization");
            initMethod.invoke(instance);
            logger.info("Called @PostConstruct initialization for " + instance.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            logger.warning("No initialization method found for " + instance.getClass().getSimpleName());
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null && cause.getMessage().contains("MongoTimeoutException")) {
                logger.severe("MongoDB connection failed for " + instance.getClass().getSimpleName() +
                             " - Make sure MongoDB is running on localhost:27017");
                // Don't re-throw, allow the service to be created but warn about DB issues
            } else {
                logger.severe("Failed to call @PostConstruct method for " + instance.getClass().getSimpleName() + ": " +
                             (cause != null ? cause.getMessage() : e.getMessage()));
                // Re-throw for other types of errors
                throw new RuntimeException("Service initialization failed", e);
            }
        } catch (Exception e) {
            logger.severe("Failed to call @PostConstruct method for " + instance.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
        // Clear cached services when type changes
        services.clear();
    }
}