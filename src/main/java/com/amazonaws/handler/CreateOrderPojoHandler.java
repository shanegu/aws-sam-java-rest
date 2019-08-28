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

package com.amazonaws.handler;

import com.amazonaws.config.DaggerOrderComponent;
import com.amazonaws.config.OrderComponent;
import com.amazonaws.dao.OrderDao;
import com.amazonaws.exception.CouldNotCreateOrderException;
import com.amazonaws.model.Order;
import com.amazonaws.model.request.CreateOrderRequest;
import com.amazonaws.model.response.ErrorMessage;
import com.amazonaws.model.response.GatewayResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.utils.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CreateOrderPojoHandler implements RequestHandler<Map<String, Object>, GatewayResponse<Object>> {
    private static final int SC_OK = 200;
    private static final int SC_CREATED = 201;
    private static final int SC_BAD_REQUEST = 400;
    private static final int SC_NOT_FOUND = 404;
    private static final int SC_CONFLICT = 409;
    private static final int SC_INTERNAL_SERVER_ERROR = 500;
    private static final ErrorMessage REQUIRE_CUSTOMER_ID_ERROR
            = new ErrorMessage("Require customerId to create an order", SC_BAD_REQUEST);
    private static final ErrorMessage REQUIRE_PRETAX_AMOUNT_ERROR
            = new ErrorMessage("Require preTaxAmount to create an order",
            SC_BAD_REQUEST);
    private static final ErrorMessage REQUIRE_POST_TAX_AMOUNT_ERROR
            = new ErrorMessage("Require postTaxAmount to create an order",
            SC_BAD_REQUEST);
    private static final Map<String, String> APPLICATION_JSON = Collections.singletonMap("Content-Type",
            "application/json");

    @Inject
    ObjectMapper objectMapper;
    @Inject
    OrderDao orderDao;
    private final OrderComponent orderComponent;

    public CreateOrderPojoHandler() {
        orderComponent = DaggerOrderComponent.builder().build();
        orderComponent.inject(this);
    }

    @Override
    public GatewayResponse<Object> handleRequest(Map<String, Object> request,
                                                 Context context) {
        if (Objects.isNull(request)) {
            return createInvalidResponse("incoming createOrderRequest is null");
        }

        Object requestBody = request.get("body");
        if (Objects.isNull(requestBody)) {
            return createInvalidResponse("createOrderRequest body is empty");
        }

        final CreateOrderRequest createOrderRequest;
        try {
            createOrderRequest = objectMapper.readValue(requestBody.toString(), CreateOrderRequest.class);
        } catch (Exception e) {
            return createInvalidResponse("Cannot deserialize order Request");
        }

        if (createOrderRequest == null) {
            return createInvalidResponse("order request is empty");
        }

        if (StringUtils.isBlank(createOrderRequest.getCustomerId())) {
            return createInvalidResponse(REQUIRE_CUSTOMER_ID_ERROR, SC_BAD_REQUEST);
        }

        if (Objects.isNull(createOrderRequest.getPreTaxAmount())) {
            return createInvalidResponse(REQUIRE_PRETAX_AMOUNT_ERROR, SC_BAD_REQUEST);
        }

        if (Objects.isNull(createOrderRequest.getPostTaxAmount())) {
            return createInvalidResponse(REQUIRE_POST_TAX_AMOUNT_ERROR, SC_BAD_REQUEST);
        }

        String createdOrder;
        try {
            final Order order = orderDao.createOrder(createOrderRequest);
            createdOrder = objectMapper.writeValueAsString(order);
        } catch (CouldNotCreateOrderException e) {
            return createInvalidResponse(new ErrorMessage(e.getMessage(), SC_INTERNAL_SERVER_ERROR),
                    SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            createdOrder = "Order created but serialization failed!";
        }

        return new GatewayResponse<>(createdOrder, APPLICATION_JSON, SC_CREATED);
    }

    private GatewayResponse<Object> createInvalidResponse(String details) {
        String errorMessage;
        try {
            errorMessage = objectMapper.writeValueAsString(new ErrorMessage("Invalid JSON in body: "
                    + details, SC_BAD_REQUEST));
        } catch (Exception e) {
            errorMessage = "Cannot serialize error message";
        }
        return new GatewayResponse<>(errorMessage, APPLICATION_JSON, SC_BAD_REQUEST);
    }

    private GatewayResponse<Object> createInvalidResponse(ErrorMessage error, int errorCode) {
        String errorMessage;
        try {
            errorMessage = objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            errorMessage = "Cannot serialize error message";
        }
        return new GatewayResponse<>(errorMessage, APPLICATION_JSON, errorCode);
    }
}
