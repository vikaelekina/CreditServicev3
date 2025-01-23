package com.example.creditservice.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrder {
    private long userId;
    private long tariffId;
}
