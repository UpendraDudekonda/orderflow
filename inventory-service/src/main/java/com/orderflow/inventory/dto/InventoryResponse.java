package com.orderflow.inventory.dto;

import java.time.LocalDateTime;

import com.orderflow.inventory.entity.Inventory;

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
public class InventoryResponse {

    private Long id;

    private Long productId;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static InventoryResponse from(
            Inventory inventory) {

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .availableQuantity(
                        inventory.getAvailableQuantity()
                )
                .reservedQuantity(
                        inventory.getReservedQuantity()
                )
                .version(inventory.getVersion())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}