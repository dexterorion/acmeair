package com.acmeair.service;

/**
 * Service provider interface for dependency injection
 */
public class ServiceProvider {

    private static final ServiceRegistry registry = ServiceRegistry.getInstance();

    /**
     * Get a service instance
     */
    public static <T> T getService(Class<T> serviceClass) {
        return registry.getService(serviceClass);
    }

    /**
     * Create services with dependencies injected
     */
    public static class Services {

        public static CustomerService customerService() {
            return getService(CustomerService.class);
        }

        public static FlightService flightService() {
            return getService(FlightService.class);
        }

        public static BookingService bookingService() {
            return getService(BookingService.class);
        }

        public static TransactionService transactionService() {
            return getService(TransactionService.class);
        }
    }
}