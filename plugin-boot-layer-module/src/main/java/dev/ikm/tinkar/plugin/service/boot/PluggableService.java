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
package dev.ikm.tinkar.plugin.service.boot;

import dev.ikm.tinkar.plugin.service.boot.internal.Layers;
import dev.ikm.tinkar.plugin.service.boot.internal.PluginWatchDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The PluggableService class represents a service that supports extensibility through plugins.
 * It provides methods to manage and access the plugins.
 * <p>
 * This class follows the Singleton design pattern to ensure that only one instance of the service exists.
 * Use the {@link #setPluginDirectory(Path)} method to initialize the service.
 * <p>
 * Use the {@link #loader(Class)} method to obtain a {@link ServiceLoader} that can be used to load plugin services
 * of a specific type.
 */
public class PluggableService {
    private static final Logger LOG = LoggerFactory.getLogger(PluggableService.class);

    private static PluggableServiceLoader pluggableServiceLoader;

    private final Layers layers;

    private static AtomicReference<PluggableService> singletonReference = new AtomicReference<>();

    /**
     * Creates an instance of the PluggableService class.
     *
     * @param pluginsDirectories a set of PluginWatchDirectory objects representing the directories where plugins are stored
     * @throws IllegalStateException if PluggableService has already been set up
     */
    private PluggableService(Set<PluginWatchDirectory> pluginsDirectories) {
        if (PluggableService.singletonReference.compareAndSet(null, this) == false) {
            throw new IllegalStateException("PluggableService must only be set up once. ");
        }
        this.layers = new Layers(pluginsDirectories);
        deployPluginServiceLoader(List.of(ModuleLayer.boot()));
    }

    public static void deployPluginServiceLoader(List<ModuleLayer> parentLayers) {
        ModuleLayer pluginServiceLoaderLayer = Layers.createModuleLayer(parentLayers,
                List.of(Path.of("/Users/kec/ikm-dev/plugin-architecture/service-plugins/plugin-service-loader-module/target/plugin-service-loader-module-1.0-SNAPSHOT.jar")));
        ServiceLoader<PluggableServiceLoader> pluggableServiceLoaderLoader =
                ServiceLoader.load(pluginServiceLoaderLayer, PluggableServiceLoader.class);
        Optional<PluggableServiceLoader> pluggableServiceLoaderOptional = pluggableServiceLoaderLoader.findFirst();
        pluggableServiceLoaderOptional.ifPresent(serviceLoader -> PluggableService.setServiceProvider(serviceLoader));
    }

    /**
     * Sets the directory where plugins are stored.
     *
     * @param pluginDirectory the path to the directory where plugins are stored
     */
    public static void setPluginDirectory(Path pluginDirectory) {
        new PluggableService(Set.of(new PluginWatchDirectory("Standard plugins directory", pluginDirectory)));
    }

    /**
     * Sets the service provider for the PluggableService.
     *
     * @param pluggableServiceLoader the PluggableServiceLoader implementation used to load service providers
     */
    public static void setServiceProvider(PluggableServiceLoader pluggableServiceLoader) {
        PluggableService.pluggableServiceLoader = pluggableServiceLoader;
    }

    /**
     * Returns a ServiceLoader for the given pluggable service class.
     *
     * @param service the pluggable service class
     * @param <S>     the type of the service
     * @return a ServiceLoader object for the given service class
     */
    public static <S> ServiceLoader<S> loader(Class<S> service) {
        if (PluggableService.pluggableServiceLoader == null) {
            throw new IllegalStateException("PluggableServiceLoader has not been set. " +
                    "Use the setServiceProvider() method to set the PluggableServiceLoader.");
        }
        if (PluggableService.pluggableServiceLoader.ensureUses(service)) {
            LOG.info("Adding uses {} to : PluggableService.pluggableServiceLoader.", service.getName());
        }
        return PluggableService.pluggableServiceLoader.loader(service);
    }
}
