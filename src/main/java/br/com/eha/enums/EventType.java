package br.com.eha.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER;

    @JsonCreator
    public static EventType fromValue(String value) {
        return EventType.valueOf(value.toUpperCase());
    }
}
