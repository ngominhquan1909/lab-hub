package com.labhub.common.protocol;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Provides a shared, pre-configured Jackson {@link ObjectMapper}.
 *
 * <p>Configuration:
 * <ul>
 *   <li>JavaTimeModule registered - {@code LocalDate} / {@code LocalDateTime} serialized as ISO strings.</li>
 *   <li>WRITE_DATES_AS_TIMESTAMPS disabled - dates are human-readable, not epoch numbers.</li>
 *   <li>FAIL_ON_UNKNOWN_PROPERTIES disabled - forward/backward protocol compatibility.</li>
 * </ul>
 */
public final class JsonMapper {

    private static final ObjectMapper INSTANCE = buildMapper();

    private JsonMapper() { }

    /**
     * Returns the shared mapper instance.
     *
     * @return configured ObjectMapper
     */
    public static ObjectMapper get() {
        return INSTANCE;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Serialize LocalDate, LocalDateTime as ISO-8601 strings
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Tolerate unknown fields - allows server/client to evolve independently
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
