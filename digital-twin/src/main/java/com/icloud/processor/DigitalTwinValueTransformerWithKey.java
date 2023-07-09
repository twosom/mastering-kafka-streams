package com.icloud.processor;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.Cancellable;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.time.Instant;

public class DigitalTwinValueTransformerWithKey
        implements ValueTransformerWithKey<String, TurbineState, DigitalTwin> {

    private ProcessorContext context;
    private KeyValueStore<String, DigitalTwin> kvStore;
    private Cancellable punctuator;

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.kvStore = context.getStateStore("digital-twin-store");
        this.punctuator = this.context.schedule(
                Duration.ofMinutes(5),
                PunctuationType.WALL_CLOCK_TIME,
                this::enforceTtl
        );
    }

    @Override
    public DigitalTwin transform(String key, TurbineState value) {
        DigitalTwin digitalTwin = kvStore.get(key);
        if (digitalTwin == null) {
            digitalTwin = DigitalTwin.empty();
        }
        digitalTwin = switch (value.type()) {
            case DESIRED -> digitalTwin.withDesired(value);
            case REPORTED -> digitalTwin.withReported(value);
        };
        kvStore.put(key, digitalTwin);
        return digitalTwin;
    }

    @Override
    public void close() {
        this.punctuator.cancel();
    }

    private void enforceTtl(long timestamp) {
        try (KeyValueIterator<String, DigitalTwin> iter = this.kvStore.all()) {
            while (iter.hasNext()) {
                KeyValue<String, DigitalTwin> entry = iter.next();
                TurbineState lastReported = entry.value.reported();
                if (lastReported == null) continue;
                if (getDaysSinceLastUpdate(lastReported) >= 7)
                    kvStore.delete(entry.key);
            }
        }
    }

    private long getDaysSinceLastUpdate(TurbineState lastReported) {
        Instant lastUpdated = Instant.parse(lastReported.timestamp());
        return Duration.between(lastUpdated, Instant.now()).toDays();
    }
}
