package dev.ikm.tinkar.serviceplugins.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMessageProvider implements MessageProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessageProvider.class);

    @Override
    public String getMessage() {
        return "Default: Welcome to JavaFX Application!";
    }
}
