package com.acmeair.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ApplicationPath;

import com.acmeair.config.AcmeAirConfiguration;
import com.acmeair.config.LoaderREST;

@ApplicationPath("/rest/info")
public class AppConfig extends Application {
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(LoaderREST.class, AcmeAirConfiguration.class));
    }
}
