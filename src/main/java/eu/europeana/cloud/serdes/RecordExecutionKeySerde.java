package eu.europeana.cloud.serdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.europeana.cloud.dto.database.RecordExecutionKey;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RecordExecutionKeySerde implements Serde<RecordExecutionKey>, Serializer<RecordExecutionKey>, Deserializer<RecordExecutionKey> {


    private final Gson gson = new GsonBuilder().create();

    @Override
    public RecordExecutionKey deserialize(String s, byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), RecordExecutionKey.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing message", e);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serde.super.configure(configs, isKey);
    }

    @Override
    public void close() {
        Serde.super.close();
    }

    @Override
    public Serializer<RecordExecutionKey> serializer() {
        return this;
    }

    @Override
    public Deserializer<RecordExecutionKey> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String s, RecordExecutionKey recordExecutionKey) {
        if (recordExecutionKey == null)
            return null;

        try {
            return gson.toJson(recordExecutionKey).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}
