package dev.ikm.tinkar.serviceplugins.demo;

import dev.ikm.tinkar.plugin.service.boot.PluggableService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.ServiceLoader;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        StringBuilder sb = new StringBuilder();
        ServiceLoader<MessageProvider> menuProviders = PluggableService.loader(MessageProvider.class);
        menuProviders.forEach(messageProvider -> {
            sb.append(messageProvider.getMessage()).append("\n");
        });
        welcomeText.setText(sb.toString());
    }
}