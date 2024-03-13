package eu.europeana.cloud.commons;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
    public static Properties getProperties(String defaultPropertyFilename, String providedPropertyFilename) throws IOException {
        if (providedPropertyFilename != null && !providedPropertyFilename.isBlank()) {
            try (FileInputStream fileInputStream = new FileInputStream(providedPropertyFilename)) {
                Properties properties = new Properties();
                properties.load(fileInputStream);
                return properties;
            }
        } else if (defaultPropertyFilename != null && !defaultPropertyFilename.isBlank()) {
            InputStream fileInputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(defaultPropertyFilename);
            Properties properties = new Properties();
            properties.load(fileInputStream);
            return properties;

        }
        throw (new FileNotFoundException("Properties file not found"));
    }
}
