import dev.ikm.tinkar.plugin.example.message.provider.PluginMessageProvider;
import dev.ikm.tinkar.serviceplugins.demo.MessageProvider;

module dev.ikm.tinkar.serviceplugins.demo.message.provider {
    requires dev.ikm.tinkar.serviceplugins.demo;
    requires org.slf4j;
    provides MessageProvider with PluginMessageProvider;
}