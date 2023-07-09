package com.icloud.processor;

import com.icloud.model.TurbineState;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class HighWindsFlatmapProcessor implements Processor<String, TurbineState, String, TurbineState> {

    private ProcessorContext<String, TurbineState> context;

    @Override
    public void init(ProcessorContext<String, TurbineState> context) {
        this.context = context;
    }

    @Override // flat map 이랑 같은 연산자 로직
    public void process(Record<String, TurbineState> record) {
        TurbineState reported = record.value();
        context.forward(record);
        if (reported.isHighWind()) {
            TurbineState desired = TurbineState.desireOff(reported);
            Record<String, TurbineState> newRecord = new Record<>(record.key(), desired, record.timestamp());
            context.forward(newRecord);
        }
    }

    @Override
    public void close() {
        // do nothing
    }
}
