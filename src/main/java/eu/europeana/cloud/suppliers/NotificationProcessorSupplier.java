package eu.europeana.cloud.suppliers;

import eu.europeana.cloud.dto.Message;
import eu.europeana.cloud.processors.NotificationProcessor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


public class NotificationProcessorSupplier implements ProcessorSupplier<String, Message, String, Message> {

    public final static String ERROR_STORE_NAME = "error-store";
    public final static String SUCCESSFUL_STORE_NAME = "success-store";

    static StoreBuilder<KeyValueStore<String, Integer>> errorStoreBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(ERROR_STORE_NAME),
            Serdes.String(),
            Serdes.Integer()
    );
    static StoreBuilder<KeyValueStore<String, Integer>> successStoreBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(SUCCESSFUL_STORE_NAME),
            Serdes.String(),
            Serdes.Integer()
    );

    private final Properties properties;

    public NotificationProcessorSupplier(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Processor<String, Message, String, Message> get() {

        return new NotificationProcessor(properties);
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        Set<StoreBuilder<?>> set = new HashSet<>();
        set.add(errorStoreBuilder);
        set.add(successStoreBuilder);
        return set;
    }
}
