package com.toasttab.example;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.*;
import javax.validation.constraints.*;

public class HelloConfiguration extends Configuration {
    @NotEmpty
    private String defaultGreeting = "Hello";


    @NotEmpty
    private String defaultSubject = "Stranger";

    @JsonProperty
    public String getDefaultGreeting() {
        return defaultGreeting;
    }

    @JsonProperty
    public String getDefaultSubject() {
        return defaultSubject;
    }
}
