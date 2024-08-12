package de.uniba.rz.backend.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
@Consumes(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class ExamplesApi extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(GetAll.class);
        resources.add(SearchByNameAndType.class);
        resources.add(SearchByName.class);
        resources.add(GetSingle.class);
        resources.add(Create.class);
        resources.add(Update.class);

        return resources;
    }
}
