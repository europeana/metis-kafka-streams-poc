package eu.europeana.cloud.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * De-serializator for {@link Message}
 */
public class MessageSerde implements Serde<Message>, Serializer<Message>, Deserializer<Message> {

    private final Gson gson = new GsonBuilder().create();


    @Override
    public void configure(Map configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String s, Message data) {
        if (data == null)
            return null;

        try {
            return gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public Message deserialize(String s, byte[] bytes) {
        if (bytes == null)
            return null;
        try {
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), Message.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing message", e);
        }
    }

    @Override
    public void close() {
        Serializer.super.close();
    }

    @Override
    public Serializer<Message> serializer() {
        return this;
    }

    @Override
    public Deserializer<Message> deserializer() {
        return this;
    }
}
