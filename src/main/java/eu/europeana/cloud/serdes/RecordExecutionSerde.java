package eu.europeana.cloud.serdes;

import com.google.gson.*;
import eu.europeana.cloud.dto.RecordExecution;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RecordExecutionSerde implements Serde<RecordExecution>, Serializer<RecordExecution>, Deserializer<RecordExecution> {


    private final Gson gson = new GsonBuilder().create();

    @Override
    public RecordExecution deserialize(String s, byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            Map<String, JsonElement> jsonElementMap = JsonParser.parseString(json).getAsJsonObject().asMap();

            RecordExecution recordExecution = new RecordExecution();

            jsonElementMap.forEach((key, value) -> {
                switch (key) {
                    case "record_data" -> recordExecution.setRecordData(value.getAsString());
                    case "execution_name" -> recordExecution.setExecutionName(value.getAsString());
                    case "execution_parameters" -> {
                        JsonObject executionParameters = null;
                        if (value.isJsonPrimitive()) {
                            executionParameters = gson.fromJson(value.getAsString(), JsonObject.class);
                        } else if (value.isJsonObject()) {
                            executionParameters = value.getAsJsonObject();
                        }
                        recordExecution.setExecutionParameters(executionParameters);
                    }
                }
            });
            return recordExecution;
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
    public Serializer<RecordExecution> serializer() {
        return this;
    }

    @Override
    public Deserializer<RecordExecution> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(String s, RecordExecution recordExecution) {
        if (recordExecution == null)
            return new byte[0];

        try {
            return gson.toJson(recordExecution).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}
