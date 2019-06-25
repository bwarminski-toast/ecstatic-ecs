package com.toasttab.example.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Greeting {
    private String message;

    public Greeting() {

    }

    public Greeting(String message) {
        this.message = message;
    }

    @JsonProperty
    public String getMessage() {
        return message;
    }

}
