package dev.ikm.tinkar.plugin.example.message.provider;

import dev.ikm.tinkar.serviceplugins.demo.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an implementation of the MessageProvider interface. It defines a simple service for providing messages.
 */
public class PluginMessageProvider implements MessageProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PluginMessageProvider.class);

    /**
     * Gets the message from the PluginMessageProvider.
     *
     * @return the message
     */
    @Override
    public String getMessage() {
        return "Hello from dynamic PluginMessageProvider :-)";
    }
}
