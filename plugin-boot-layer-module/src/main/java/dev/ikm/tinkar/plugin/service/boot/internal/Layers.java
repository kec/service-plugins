/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.plugin.service.boot.internal;

import dev.ikm.tinkar.plugin.service.boot.*;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The Layers class represents a system of module layers used to manage plugins in an application.
 * It provides functionality for setting up and configuring the layers, deploying plugins, and handling
 * directory change events.
 */
public class Layers {
    private static final Logger LOG = LoggerFactory.getLogger(Layers.class);
    private static final Pattern PLUGIN_ARTIFACT_PATTERN = Pattern.compile("(.*?)\\-(\\d[\\d+\\-_A-Za-z\\.]*?)\\.(jar|zip|tar|tar\\.gz)");
    public static final String TINKAR_PLUGINS_TEMP_DIR = "tinkar-plugins";
    public static final String BOOT_LAYER = "boot-layer";
    public static final String PLUGIN_SERVICE_LOADER_LAYER = "plugin-service-loader-layer";

    /**
     * The actual module layers by name.
     */
    private final CopyOnWriteArraySet<PluginNameAndModuleLayer> moduleLayers = new CopyOnWriteArraySet<>();

    /**
     * Temporary directory where all plug-ins will be copied to. Modules will be
     * sourced from there, allowing to remove plug-ins by deleting their original
     * directory.
     */
    private final Path pluginsWorkingDir;

    /**
     * All configured directories potentially containing plug-ins.
     */
    private final Set<PluginWatchDirectory> pluginsDirectories;

    private static final List<ModuleLayer> pluginParentLayerAsList = List.of(ModuleLayer.boot());

    private int pluginIndex = 0;

    /**
     * Creates a new instance of Layers.
     *
     * @param pluginsDirectories a set of PluginsDirectory objects representing the directories where plugins are stored
     */
    public Layers(Set<PluginWatchDirectory> pluginsDirectories) {
        this.moduleLayers.add(new PluginNameAndModuleLayer(BOOT_LAYER, ModuleLayer.boot()));
        this.pluginsDirectories = Collections.unmodifiableSet(pluginsDirectories);

        try {
            this.pluginsWorkingDir = Files.createTempDirectory(TINKAR_PLUGINS_TEMP_DIR);

            if (!pluginsDirectories.isEmpty()) {
                Deployer deployer = new Deployer(pluginsDirectories);
                for (PluginWatchDirectory pluginWatchDirectory: pluginsDirectories) {
                    List<PluginNameAndModuleLayer> newPluginModuleLayers = handlePluginComponent(pluginWatchDirectory);
                    newPluginModuleLayers.forEach(pluginNameAndModuleLayer -> deployer.deploy(pluginNameAndModuleLayer));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the plugin component by creating module layer for each plugin artifact found in the directory.
     *
     * @param plugin the plugin object representing the plugin component
     * @return a map of plugin names and associated module layers
     * @throws IOException if an I/O error occurs while handling the plugin component
     */
    private List<PluginNameAndModuleLayer> handlePluginComponent(PluginWatchDirectory plugin) throws IOException {
        List<PluginNameAndModuleLayer> newPluginModuleLayers = new ArrayList<>();

        // Expect .jar, .zip, .tar, .tar.gz as direct children
        Files.list(plugin.directory())
                .forEach(path -> {
                    Matcher matcher = PLUGIN_ARTIFACT_PATTERN.matcher(path.getFileName().toString());
                    if (!matcher.matches()) {
                        return;
                    }

                    String pluginArtifactId = matcher.group(1);
                    String pluginVersion = matcher.group(2);
                    String derivedFrom = plugin.directory().getFileName().toString();
                    String pluginName = String.join("-", derivedFrom, pluginArtifactId, pluginVersion);

                    Path pluginDir = pluginsWorkingDir.resolve(pluginIndex++ + "-" + pluginName);
                    List<Path> modulePathEntries = unpackPluginArtifact(path, pluginDir);

                    ModuleLayer moduleLayer = createModuleLayer(pluginParentLayerAsList, modulePathEntries);

                    PluginNameAndModuleLayer pluginNameAndModuleLayer = new PluginNameAndModuleLayer(pluginName, moduleLayer);
                    moduleLayers.add(pluginNameAndModuleLayer);
                    newPluginModuleLayers.add(pluginNameAndModuleLayer);
                });

        return newPluginModuleLayers;
    }

    /**
     * Creates a module layer with the given parent layers and module path entries.
     *
     * @param parentLayers      the list of parent module layers
     * @param modulePathEntries the list of module path entries
     * @return the created module layer
     */
    public static ModuleLayer createModuleLayer(List<ModuleLayer> parentLayers, List<Path> modulePathEntries) {
        ClassLoader scl = ClassLoader.getSystemClassLoader();

        ModuleFinder finder = ModuleFinder.of(modulePathEntries.toArray(Path[]::new));

        Set<String> roots = finder.findAll()
                .stream()
                .map(m -> m.descriptor().name())
                .collect(Collectors.toSet());

        Configuration appConfig = Configuration.resolve(
                finder,
                parentLayers.stream().map(ModuleLayer::configuration).collect(Collectors.toList()),
                ModuleFinder.of(),
                roots);

        return ModuleLayer.defineModulesWithOneLoader(appConfig, parentLayers, scl).layer();
    }

    /**
     * Unpacks a plugin artifact to the target directory.
     *
     * @param pluginArtifact the path of the plugin artifact to unpack
     * @param targetDir the directory to unpack the plugin artifact to
     *
     * @return a list containing the target directory
     *
     * @throws UnsupportedOperationException if the plugin artifact has an unsupported file extension
     */
    private List<Path> unpackPluginArtifact(Path pluginArtifact, Path targetDir) {
        String fileName = pluginArtifact.getFileName().toString();
        if (fileName.endsWith(".jar")) {
            Path dest = targetDir.resolve(fileName);
            try {
                Files.createDirectories(dest.getParent());
                Files.copy(pluginArtifact, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".tar")) {
            throw new UnsupportedOperationException("Can't handle .tar");
        } else if (fileName.endsWith(".tar.gz")) {
            throw new UnsupportedOperationException("Can't handle .tar.gz");
        }

        return List.of(targetDir);
    }

    /**
     * The Deployer class is responsible for managing the deployment of plugins by monitoring changes in a directory and
     * creating or removing plugin module layers accordingly.
     */
    private class Deployer {

        private final PluginLifecycleSupport pluginLifecycleSupport = new PluginLifecycleSupport();

        /**
         * Deploys plugins from the given set of plugin directories.
         *
         * @param pluginsDirectories the set of plugin directories
         */
        public Deployer(Set<PluginWatchDirectory> pluginsDirectories) {

            ExecutorService executor = Executors.newFixedThreadPool(pluginsDirectories.size(),
                    runnable -> Thread.ofVirtual().name("Plugin directory watcher thread").unstarted(runnable));

            for (PluginWatchDirectory pluginDirectory : pluginsDirectories) {
                executor.execute(() -> {
                    try {
                        DirectoryWatcher watcher = DirectoryWatcher.builder()
                                .path(pluginDirectory.directory())
                                .listener(event -> onDirectoryChange(event, pluginDirectory))
                                .build();

                        watcher.watch();
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        // TODO: handle exception as it will be lost inside the Executor IIRC
                        throw new RuntimeException(e);
                    }
                });
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        LOG.warn("Executor did not terminate in the specified time.");
                    }
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }));
        }

        /**
         * Handles directory change events.
         *
         * @param event           the directory change event
         * @param pluginDirectory the plugin directory
         */
        private void onDirectoryChange(DirectoryChangeEvent event, PluginWatchDirectory pluginDirectory) {
            Matcher matcher = PLUGIN_ARTIFACT_PATTERN.matcher(event.path().getFileName().toString());
            if (!matcher.matches()) {
                return;
            }

            String pluginArtifactId = matcher.group(1);
            String pluginVersion = matcher.group(2);
            String derivedFrom = pluginDirectory.name();
            String pluginLayerName = String.join("-", derivedFrom, pluginArtifactId, pluginVersion);

            switch (event.eventType()) {
                case CREATE -> {
                    if (moduleLayers.stream().noneMatch(pluginNameAndModuleLayer -> pluginNameAndModuleLayer.name().equals(pluginLayerName))) {
                        Path pluginDir = pluginsWorkingDir.resolve(pluginIndex++ + "-" + pluginLayerName);
                        List<Path> modulePathEntries = unpackPluginArtifact(event.path(), pluginDir);
                        ModuleLayer moduleLayer = createModuleLayer(pluginParentLayerAsList, modulePathEntries);
                        PluginNameAndModuleLayer pluginNameAndModuleLayer = new PluginNameAndModuleLayer(pluginLayerName, moduleLayer);
                        moduleLayers.add(pluginNameAndModuleLayer);
                        // Create new service loader with new layer...
                        PluggableService.deployPluginServiceLoader(moduleLayers.stream().map(pluginNameAndModuleLayerFromStream -> pluginNameAndModuleLayerFromStream.moduleLayer()).toList());
                        deploy(pluginNameAndModuleLayer);
                    } else {
                        LOG.warn("Trying to create a layer that already exists: " + pluginLayerName);
                    }
                }
                case DELETE -> {
                    Optional<PluginNameAndModuleLayer> match = moduleLayers.stream().filter(pluginNameAndModuleLayer -> pluginNameAndModuleLayer.name().equals(pluginLayerName)).findFirst();
                    match.ifPresentOrElse(pluginNameAndModuleLayer -> {
                        moduleLayers.remove(pluginNameAndModuleLayer);
                        // Create new service loader without the removed layer...
                        PluggableService.deployPluginServiceLoader(moduleLayers.stream().map(pluginNameAndModuleLayerFromStream -> pluginNameAndModuleLayerFromStream.moduleLayer()).toList());
                        undeploy(pluginNameAndModuleLayer);
                    },
                        //else
                        () -> LOG.warn("Trying to delete a layer that is not present: " + pluginLayerName));
                }
                default -> LOG.warn("Unexpected DirectoryChange event type: " + event);
            }
        }

        /**
         * Deploys a plugin layer by notifying any potential lifecycle listeners about the new layer.
         *
         * @param pluginLayerToDeploy the PluginNameAndModuleLayer representing the plugin layer to deploy
         */
        public void deploy(PluginNameAndModuleLayer pluginLayerToDeploy) {
            // for each existing layer, notify any potential lifecycle listeners about the new layer
            for (PluginNameAndModuleLayer pluginNameAndModuleLayer : moduleLayers) {
                pluginLifecycleSupport.notifyPluginLayerListenersOnAddition(pluginNameAndModuleLayer.moduleLayer(),
                        pluginLayerToDeploy);
            }
        }

        /**
         * Undeploy the specified plugin layer by notifying any potential lifecycle listeners about the removed layer.
         *
         * @param pluginLayerToUndeploy the plugin layer to undeploy
         */
        public void undeploy(PluginNameAndModuleLayer pluginLayerToUndeploy) {
            // for each existing layer, notify any potential lifecycle listeners about the removed layer
            for (PluginNameAndModuleLayer pluginNameAndModuleLayer : moduleLayers) {
                pluginLifecycleSupport.notifyPluginLayerListenersOnRemoval(pluginNameAndModuleLayer.moduleLayer(),
                        pluginLayerToUndeploy);
            }
        }

    }
}
