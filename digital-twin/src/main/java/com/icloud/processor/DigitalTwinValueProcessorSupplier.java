package com.icloud.processor;

import com.icloud.model.DigitalTwin;
import com.icloud.model.TurbineState;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;

public class DigitalTwinValueProcessorSupplier implements FixedKeyProcessorSupplier<String, TurbineState, DigitalTwin> {
    @Override
    public FixedKeyProcessor<String, TurbineState, DigitalTwin> get() {
        return new DigitalTwinValueProcessor();
    }
}
