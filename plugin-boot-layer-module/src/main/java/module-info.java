import dev.ikm.tinkar.plugin.service.boot.PluggableService;
import dev.ikm.tinkar.plugin.service.boot.PluggableServiceLoader;
import dev.ikm.tinkar.plugin.service.boot.PluginLifecycleListener;
import dev.ikm.tinkar.plugin.service.boot.internal.PluginLifecycleListenerLogger;

module dev.ikm.tinkar.plugin.service.boot {
    exports dev.ikm.tinkar.plugin.service.boot;
    requires dev.ikm.jpms.directory.watcher;
    requires org.slf4j;

    uses PluggableServiceLoader;
    uses PluginLifecycleListener;

    provides PluginLifecycleListener with PluginLifecycleListenerLogger;
}