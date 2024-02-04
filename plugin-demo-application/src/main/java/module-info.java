import dev.ikm.tinkar.plugin.service.boot.PluggableServiceLoader;
import dev.ikm.tinkar.serviceplugins.demo.DefaultMessageProvider;
import dev.ikm.tinkar.serviceplugins.demo.MessageProvider;

module dev.ikm.tinkar.serviceplugins.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires dev.ikm.tinkar.plugin.service.boot;
    requires org.slf4j;

    opens dev.ikm.tinkar.serviceplugins.demo to javafx.fxml;
    exports dev.ikm.tinkar.serviceplugins.demo;

    uses PluggableServiceLoader;
    uses org.slf4j.spi.SLF4JServiceProvider;
    uses MessageProvider;

    provides MessageProvider with DefaultMessageProvider;
}