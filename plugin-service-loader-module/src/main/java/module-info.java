import dev.ikm.tinkar.plugin.service.boot.PluggableServiceLoader;
import dev.ikm.tinkar.plugin.service.boot.PluginLifecycleListener;
import dev.ikm.tinkar.plugin.service.loader.PluginServiceLoader;

module dev.ikm.tinkar.plugin.service.loader {
    requires dev.ikm.tinkar.plugin.service.boot;
    requires org.slf4j;

    provides PluggableServiceLoader with PluginServiceLoader;
    provides PluginLifecycleListener with PluginServiceLoader;

    uses PluginLifecycleListener;
}