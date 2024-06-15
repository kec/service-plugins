package dev.ikm.tinkar.serviceplugins.demo;

import dev.ikm.tinkar.plugin.service.boot.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class HelloApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(HelloApplication.class);
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // setup plugin layers
        LOG.info("Starting HelloApplication");

        // setup plugin directory.
        LOG.info("Application working directory: " + System.getProperties().getProperty("user.dir"));
        Path workingPath = Path.of(System.getProperties().getProperty("user.dir"));
        Path pluginPath = workingPath.resolve(Path.of("target/plugins"));
        pluginPath.toFile().mkdirs();
        LOG.info("Plugin directory: " + pluginPath.toAbsolutePath());
        PluggableService.setPluginDirectory(pluginPath);

        // launch application
        launch();
    }
}