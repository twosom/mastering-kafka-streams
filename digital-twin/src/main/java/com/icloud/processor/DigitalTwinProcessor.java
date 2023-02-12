package com.icloud.processor;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.Instant;

public class DigitalTwinProcessor implements Processor<String, TurbineState, String, DigitalTwin> {

    private ProcessorContext<String, DigitalTwin> context;
    private KeyValueStore<String, DigitalTwin> kVStore;
    private Cancellable punctuator;

    @Override
    public void init(ProcessorContext<String, DigitalTwin> context) {
        this.context = context;
        this.kVStore = context.getStateStore("digital-twin-store");
        this.punctuator = context.schedule(
                Duration.ofMinutes(5),
                PunctuationType.WALL_CLOCK_TIME,
                this::enforceTtl
        );
    }

    private void enforceTtl(Long timestamp) {
        try (final KeyValueIterator<String, DigitalTwin> iter = kVStore.all()) {
            while (iter.hasNext()) {
                final KeyValue<String, DigitalTwin> entry = iter.next();
                final TurbineState lastReportedState = entry.value.reported();
                if (lastReportedState == null) continue;
                final Instant lastUpdated = Instant.parse(lastReportedState.timestamp());
                long daysSinceLastUpdate = Duration.between(lastUpdated, Instant.now()).toDays();
                if (daysSinceLastUpdate >= 7) kVStore.delete(entry.key);
            }
        }
    }

    @Override
    public void process(Record<String, TurbineState> record) {
        final String key = record.key();
        final TurbineState value = record.value();
        DigitalTwin digitalTwin = kVStore.get(key);
        if (digitalTwin == null) {
            digitalTwin = DigitalTwin.empty();
        }

        digitalTwin = switch (value.type()) {
            case DESIRED -> digitalTwin.withDesired(value);
            case REPORTED -> digitalTwin.withReported(value);
        };

        kVStore.put(key, digitalTwin);
        final Record<String, DigitalTwin> newRecord = new Record<>(record.key(), digitalTwin, record.timestamp());
        context.forward(newRecord);
    }

    @Override
    public void close() {
        punctuator.cancel();
    }
}
