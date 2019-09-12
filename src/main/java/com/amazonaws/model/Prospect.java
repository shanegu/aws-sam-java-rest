package com.amazonaws.model;

import com.amazonaws.data.request.CreateProspectRequest;
import com.amazonaws.model.events.ProspectCreatedEvent;
import software.amazon.awssdk.utils.StringUtils;

import java.util.UUID;

public class Prospect implements Aggregate {
    private static final String CREATED_EVENT_NAME = "prospectCreated";
    public boolean validate(final CreateProspectRequest createProspectRequest) {
        if (StringUtils.isBlank(createProspectRequest.getFirstName()) ||
                StringUtils.isBlank(createProspectRequest.getLastName())) {
            return false;
        }
        return true;
    }

    public ProspectCreatedEvent generateCreatedEvent(final CreateProspectRequest createProspectRequest) {
        return ProspectCreatedEvent.builder()
                .id(UUID.randomUUID().toString())
                .prospectID(UUID.randomUUID().toString())
                .firstName(createProspectRequest.getFirstName())
                .lastName(createProspectRequest.getLastName())
                .phoneNumber(createProspectRequest.getPhoneNumber())
                .eventName(CREATED_EVENT_NAME)
                .version(1L)
                .build();
    }
}
