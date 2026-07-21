package br.com.eha.dto.request;

import br.com.eha.enums.EventType;
import lombok.Data;

@Data
public class EventRequest {

    private EventType type;
    private Integer amount;
    private String origin;
    private String destination;
}