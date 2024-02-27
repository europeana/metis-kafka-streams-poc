package eu.europeana.cloud.processors.mediaProcessors;

import eu.europeana.cloud.dto.queues.Message;
import eu.europeana.cloud.suppliers.NotificationProcessorSupplier;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

import static eu.europeana.cloud.suppliers.NotificationProcessorSupplier.ERROR_STORE_NAME;
import static eu.europeana.cloud.suppliers.NotificationProcessorSupplier.SUCCESSFUL_STORE_NAME;

public class NotificationProcessor extends MediaProcessor implements
        Processor<String, Message, String, Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessorSupplier.class);
    private ProcessorContext<String, Message> context;
    private KeyValueStore<String, Integer> errorStore;
    private KeyValueStore<String, Integer> successStore;


    public NotificationProcessor(Properties topologyProperties) {
        super(topologyProperties);
    }


    // My idea is to use scheduled tasks to accumulate cassandra queries regarding given task
    // and then input them at once, We don't need to worry about race conditions etp since
    // each partition is processed at single thread. Data before getting to that processor
    // is re-keyed so each task with given ID got same key meaning that it will be processed at same partition
    // It is still not ideal solution, but it is better than our currently implemented one in storm
    // I didn't manage to finish inputting real diagnostic information because I ran out of time
    private void inputNotification(final long timestamp) {
        LOGGER.info("Start process of updating information form keystores");
        LOGGER.info("Error store:");
        try (KeyValueIterator<String, Integer> iterator = errorStore.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, Integer> next = iterator.next();
                LOGGER.info("Key: {}, Value: {}", next.key, next.value);
                errorStore.delete(next.key);
            }
        }

        LOGGER.info("Success store:");
        try (KeyValueIterator<String, Integer> iterator = successStore.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, Integer> next = iterator.next();
                LOGGER.info("Key: {}, Value: {}", next.key, next.value);
                successStore.delete(next.key);
            }
        }

    }

    @Override
    public void init(ProcessorContext<String, Message> context) {
        Processor.super.init(context);
        this.context = context;
        this.errorStore = context.getStateStore(ERROR_STORE_NAME);
        this.successStore = context.getStateStore(SUCCESSFUL_STORE_NAME);
        this.context.schedule(Duration.ofSeconds(30), PunctuationType.STREAM_TIME, this::inputNotification);
    }

    @Override
    public void process(Record<String, Message> record) {
        LOGGER.info("Received key: {} value: {}", record.key(), record.value());
        if (record.key() == null) {
            LOGGER.error("Received record with null key");
        } else {
            if (record.value().getErrorInformation() != null) {
                incrementStoredValue(errorStore, record);
            } else {
                incrementStoredValue(successStore, record);
            }
        }
    }

    private void incrementStoredValue(KeyValueStore<String, Integer> store, Record<String, Message> record) {
        Integer currentCount = store.get(record.key());
        if (currentCount == null) {
            currentCount = 1;
        } else {
            currentCount++;
        }
        store.put(record.key(), currentCount);
    }

    @Override
    public void close() {
        Processor.super.close();
    }
}
