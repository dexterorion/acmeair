package com.acmeair.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/rest/api")
public class AcmeAirApp extends Application {
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(BookingsREST.class, CustomerREST.class, FlightsREST.class, LoginREST.class));
    }
}
