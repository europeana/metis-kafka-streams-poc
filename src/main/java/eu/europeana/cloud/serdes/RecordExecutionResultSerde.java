package eu.europeana.cloud.serdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.europeana.cloud.dto.database.RecordExecutionResult;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RecordExecutionResultSerde implements Serde<RecordExecutionResult>, Serializer<RecordExecutionResult>, Deserializer<RecordExecutionResult> {
    private final Gson gson = new GsonBuilder().create();

    @Override
    public RecordExecutionResult deserialize(String s, byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), RecordExecutionResult.class);
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
    public Serializer<RecordExecutionResult> serializer() {
        return this;
    }

    @Override
    public Deserializer<RecordExecutionResult> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String s, RecordExecutionResult recordExecutionResult) {
        if (recordExecutionResult == null)
            return new byte[0];

        try {
            return gson.toJson(recordExecutionResult).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}
