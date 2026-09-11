package com.orderflow.order.dto;

import java.math.BigDecimal;

import com.orderflow.order.entity.OrderItem;

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
public class OrderItemResponse {

    private Long id;

    private Long productId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {

        BigDecimal subtotal =
                item.getUnitPrice()
                        .multiply(
                                BigDecimal.valueOf(item.getQuantity())
                        );

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(subtotal)
                .build();
    }
}