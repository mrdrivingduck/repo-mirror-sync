package iot.zjt;

import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;

/**
 * Initialize the logger configuration from resource file.
 *
 * @author Mr Dk.
 * @since 2020/12/28
 */
public class Logger {
    public static void init() throws IOException {
        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.Log4j2LogDelegateFactory");
        System.setProperty("log4j2.skipJansi", "false"); // VM options
        System.setProperty("sun.stdout.encoding", "UTF-8");
        ConfigurationSource source = new ConfigurationSource(Logger.class.getResourceAsStream("/log4j2.xml"));
        Configurator.initialize(null, source);
    }
}
