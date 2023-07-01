package com.icloud.model;

public record BodyTemp(String timestamp, Double temperature, String unit) implements Vital {}
