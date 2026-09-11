package com.orderflow.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOperationResponse {

    private Long productId;

    private String message;

    private Integer availableQuantity;

    private Integer reservedQuantity;
}