/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazonaws.dao;

import com.amazonaws.exception.CouldNotCreateOrderException;
import com.amazonaws.exception.TableDoesNotExistException;
import com.amazonaws.model.events.ProspectCreatedEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class ProspectCreatedEventDao {

    private static final String UPDATE_EXPRESSION
            = "SET customerId = :cid, preTaxAmount = :pre, postTaxAmount = :post ADD version :o";

    private static final String EVENT_ID = "eventID";

    private final String tableName;
    private final DynamoDbClient dynamoDb;
    private final int pageSize;

    /**
     * Constructs an OrderDao.
     *
     * @param dynamoDb  dynamodb client
     * @param tableName name of table to use for orders
     * @param pageSize  size of pages for getOrders
     */
    public ProspectCreatedEventDao(final DynamoDbClient dynamoDb, final String tableName,
                                   final int pageSize) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.pageSize = pageSize;
    }

    private Map<String, AttributeValue> createEventItem(final ProspectCreatedEvent prospectCreatedEvent) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(EVENT_ID, AttributeValue.builder().s(prospectCreatedEvent.getId()).build());
        item.put("version", AttributeValue.builder().n("1").build());
        item.put("prospectID",
                AttributeValue.builder().s(prospectCreatedEvent.getProspectID()).build());
        item.put("version", AttributeValue.builder().n("1").build());
        item.put("eventName", AttributeValue.builder().s(prospectCreatedEvent.getEventName()).build());

        return item;
    }

    /**
     * Creates a new ProspectCreatedEvent.
     *
     * @param prospectCreatedEvent details of event to create
     * @return created event ID
     */
    public String addProspectCreatedEvent(final ProspectCreatedEvent prospectCreatedEvent) {

        int tries = 0;
        while (tries < 10) {
            try {
                final Map<String, AttributeValue> item = createEventItem(prospectCreatedEvent);
                final PutItemRequest putItemRequest = PutItemRequest.builder()
                        .tableName(tableName)
                        .item(item)
                        .conditionExpression("attribute_not_exists(eventID)")
                        .build();
                dynamoDb.putItem(putItemRequest);
                return prospectCreatedEvent.getId();
            } catch (ConditionalCheckFailedException e) {
                tries++;
            } catch (ResourceNotFoundException e) {
                throw new TableDoesNotExistException(
                        "Event table " + tableName + " does not exist");
            }
        }
        throw new CouldNotCreateOrderException(
                "Unable to generate unique event id after 10 tries");
    }

    private static boolean isNullOrEmpty(final String string) {
        return string == null || string.isEmpty();
    }
}

