package com.icloud.processor;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.Instant;

public class DigitalTwinValueProcessor implements FixedKeyProcessor<String, TurbineState, DigitalTwin> {
    private FixedKeyProcessorContext<String, DigitalTwin> context;
    private KeyValueStore<String, DigitalTwin> kVStore;
    private Cancellable punctuator;


    @Override
    public void init(FixedKeyProcessorContext<String, DigitalTwin> context) {
        this.context = context;
        this.kVStore = context.getStateStore("digital-twin-store");
        this.punctuator = context.schedule(
                Duration.ofMinutes(5),
                PunctuationType.WALL_CLOCK_TIME,
                this::enforceTtl
        );
    }

    private void enforceTtl(long timestamp) {
        try (KeyValueIterator<String, DigitalTwin> iter = kVStore.all()) {
            while (iter.hasNext()) {
                final KeyValue<String, DigitalTwin> entry = iter.next();
                final TurbineState lastReportedState = entry.value.reported();
                if (lastReportedState == null) continue;
                final Instant lastUpdated = Instant.parse(lastReportedState.timestamp());
                final long daysSinceLastUpdate = Duration.between(lastUpdated, Instant.now()).toDays();
                if (daysSinceLastUpdate >= 7) kVStore.delete(entry.key);
            }
        }
    }

    @Override
    public void process(FixedKeyRecord<String, TurbineState> record) {
        final String key = record.key();
        TurbineState value = record.value();
        DigitalTwin digitalTwin = kVStore.get(key);
        if (digitalTwin == null) digitalTwin = DigitalTwin.empty();
        digitalTwin = switch (value.type()) {

            case DESIRED -> digitalTwin.withDesired(value);
            case REPORTED -> digitalTwin.withReported(value);
        };
        kVStore.put(key, digitalTwin);
        final FixedKeyRecord<String, DigitalTwin> newRecord = record.withValue(digitalTwin);
        context.forward(newRecord);
    }

    @Override
    public void close() {
        punctuator.cancel();
    }
}
