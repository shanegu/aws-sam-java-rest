package com.amazonaws.data;

import lombok.Setter;

import java.sql.Timestamp;

@Setter
public class Command {
    private String commandID;
    private Timestamp createDateTime;
    private String userID;
}
