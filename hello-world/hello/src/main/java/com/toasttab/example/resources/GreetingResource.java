package com.toasttab.example.resources;

import com.toasttab.example.api.Greeting;

import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/greeting")
@Produces(MediaType.APPLICATION_JSON)
public class GreetingResource {
    private final String defaultSubject;
    private final String defaultGreeting;

    public GreetingResource(String defaultSubject, String defaultGreeting) {
        this.defaultSubject = defaultSubject;
        this.defaultGreeting = defaultGreeting;
    }

    @GET
    public Greeting sayHello(@QueryParam("greeting") Optional<String> greeting, @QueryParam("name") Optional<String> name) {
        final String value = String.format("%s, %s!", greeting.orElse(defaultGreeting), name.orElse(defaultSubject));
        return new Greeting(value);
    }
}
