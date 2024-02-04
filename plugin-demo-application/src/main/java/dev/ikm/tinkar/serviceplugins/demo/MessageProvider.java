package dev.ikm.tinkar.serviceplugins.demo;

/**
 * A simple service definition for dynamic pluggable service loading.
 */
public interface MessageProvider {

    /**
     * Gets the message from the MessageProvider.
     *
     * @return the message
     */
    String getMessage();
}
