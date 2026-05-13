package ru.practicum.kafka.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private BinaryEncoder encoder;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) {
            return null;
        }
        log.debug(
                "Serializing AVRO data for topic {}, class: {}",
                topic,
                data.getClass().getSimpleName());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            encoder = encoderFactory.binaryEncoder(out, encoder);
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new SerializationException("Serializing error for topik [" + topic + "]", ex);
        }
    }

    @Override
    public void close() {}
}
