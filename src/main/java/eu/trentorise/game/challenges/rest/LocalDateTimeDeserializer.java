package eu.trentorise.game.challenges.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.joda.time.format.DateTimeFormat;

import java.io.IOException;

public class LocalDateTimeDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").parseDateTime(key);
    }

}
