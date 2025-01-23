package com.example.creditservice.model.response;

import com.example.creditservice.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataResponseStatus {
    OrderStatus orderStatus;
}
