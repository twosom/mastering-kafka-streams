package com.icloud.model;

import lombok.Data;

@Data
public class BodyTemp implements Vital {
    private String timestamp;
    private Double temperature;
    private String unit;
}
