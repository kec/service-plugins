package dev.ikm.tinkar.plugin.example.message.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * This class contains the tests for the PluginMessageProvider class. 
 * The tests focus on the getMessage() method.
 */
public class PluginMessageProviderTest {
    
    /**
     * Test the getMessage() method of the PluginMessageProvider for normal conditions.
     * The expected message is "Hello from dynamic PluginMessageProvider :-)".
     */
    @Test
    public void testGetMessage() {
        PluginMessageProvider provider = new PluginMessageProvider();
        String expectedMessage = "Hello from dynamic PluginMessageProvider :-)";
        assertEquals(expectedMessage, provider.getMessage());
    }
}