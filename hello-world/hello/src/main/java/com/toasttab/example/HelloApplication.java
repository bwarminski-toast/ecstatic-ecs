package com.toasttab.example;

import com.toasttab.example.api.Greeting;
import com.toasttab.example.resources.GreetingResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class HelloApplication extends Application<HelloConfiguration> {

    public static void main(final String[] args) throws Exception {
        new HelloApplication().run(args);
    }

    @Override
    public String getName() {
        return "hello";
    }

    @Override
    public void initialize(final Bootstrap<HelloConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final HelloConfiguration configuration,
                    final Environment environment) {
        final GreetingResource resource = new GreetingResource(
                configuration.getDefaultSubject(),
                configuration.getDefaultGreeting()
        );
        environment.jersey().register(resource);
    }

}
