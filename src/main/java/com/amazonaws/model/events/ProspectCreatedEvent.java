package com.amazonaws.model.events;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProspectCreatedEvent {
    private final String id;
    private final String prospectID;
    private final String firstName;
    private final String lastName;
    private final String phoneNumber;
    private final String eventName;
    private final String commandID;
    private Long version;
}
