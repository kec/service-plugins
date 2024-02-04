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

import dev.ikm.tinkar.plugin.service.boot.PluginLifecycleListener;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Invoked by the launcher whenever a plugin layer gets added or removed.
 * Invokes all registered {@link PluginLifecycleListener}s in turn.
 */
public class PluginLifecycleSupport {

    /**
     * Notifies registered PluginLifecycleListeners about the addition of a plugin layer.
     *
     * @param listenerModuleLayer        the module layer of the listener
     * @param pluginNameAndModuleLayer   the name and module layer of the plugin being added
     *
     * @throws NullPointerException if either listenerModuleLayer or pluginNameAndModuleLayer is null
     */
    public void notifyPluginLayerListenersOnAddition(ModuleLayer listenerModuleLayer, PluginNameAndModuleLayer pluginNameAndModuleLayer) {
        ServiceLoader<PluginLifecycleListener> loader = ServiceLoader.load(listenerModuleLayer, PluginLifecycleListener.class);

        Iterator<PluginLifecycleListener> listeners = loader.iterator();
        while (listeners.hasNext()) {
            PluginLifecycleListener listener = listeners.next();

            // notify each listener only through its defining layer, but not via other layers
            // derived from that
            if (listener.getClass().getModule().getLayer().equals(listenerModuleLayer)) {
                listener.pluginLayerAdded(pluginNameAndModuleLayer.name(), pluginNameAndModuleLayer.moduleLayer());
            }
        }
    }

    /**
     * Notifies registered plugin layer listeners about the removal of a plugin layer.
     *
     * @param listenerModuleLayer the module layer of the listener
     * @param pluginNameAndModuleLayer the name and module layer of the plugin being removed
     */
    public void notifyPluginLayerListenersOnRemoval(ModuleLayer listenerModuleLayer, PluginNameAndModuleLayer pluginNameAndModuleLayer) {
        ServiceLoader<PluginLifecycleListener> loader = ServiceLoader.load(listenerModuleLayer, PluginLifecycleListener.class);

        Iterator<PluginLifecycleListener> listeners = loader.iterator();
        while (listeners.hasNext()) {
            PluginLifecycleListener listener = listeners.next();

            // notify each listener only through its defining layer, but not via other layers derived from that
            if (listener.getClass().getModule().getLayer().equals(listenerModuleLayer)) {
                listener.pluginLayerBeingRemoved(pluginNameAndModuleLayer.name(), pluginNameAndModuleLayer.moduleLayer());
            }
        }
    }
}
