package eu.europeana.cloud.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtil.class);
    public static Properties getProperties(String defaultPropertyFilename, String providedPropertyFilename) throws IOException {
        if (providedPropertyFilename != null && !providedPropertyFilename.isBlank()) {
            LOGGER.info("Using provided property file: {}", providedPropertyFilename);
            try (FileInputStream fileInputStream = new FileInputStream(providedPropertyFilename)) {
                Properties properties = new Properties();
                properties.load(fileInputStream);
                return properties;
            }
        } else if (defaultPropertyFilename != null && !defaultPropertyFilename.isBlank()) {
            LOGGER.info("Using default property file: {}", defaultPropertyFilename);
            InputStream fileInputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(defaultPropertyFilename);
            Properties properties = new Properties();
            properties.load(fileInputStream);
            return properties;

        }
        throw (new FileNotFoundException("Properties file not found"));
    }
}
