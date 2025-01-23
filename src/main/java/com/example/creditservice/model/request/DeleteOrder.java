package com.example.creditservice.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DeleteOrder {
    private long userId;
    private UUID orderId;
}
