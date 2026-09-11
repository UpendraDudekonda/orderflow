package com.orderflow.inventory.service;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.inventory.dto.CreateInventoryRequest;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockOperationResponse;
import com.orderflow.inventory.dto.UpdateInventoryRequest;
import com.orderflow.inventory.entity.Inventory;
import com.orderflow.inventory.exception.ConcurrentStockUpdateException;
import com.orderflow.inventory.exception.InsufficientStockException;
import com.orderflow.inventory.exception.InventoryNotFoundException;
import com.orderflow.inventory.repository.InventoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;


    @Transactional
    public InventoryResponse createInventory(
            CreateInventoryRequest request) {

        if (inventoryRepository.existsByProductId(
                request.getProductId())) {

            throw new IllegalArgumentException(
                    "Inventory already exists for product: "
                            + request.getProductId()
            );
        }

        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .availableQuantity(request.getQuantity())
                .reservedQuantity(0)
                .build();

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return InventoryResponse.from(savedInventory);
    }


    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(
            Long productId) {

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(
                                () -> new InventoryNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        return InventoryResponse.from(inventory);
    }


    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {

        return inventoryRepository.findAll()
                .stream()
                .map(InventoryResponse::from)
                .toList();
    }


    @Transactional
    public InventoryResponse updateInventory(
            Long productId,
            UpdateInventoryRequest request) {

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(
                                () -> new InventoryNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        inventory.setAvailableQuantity(
                request.getQuantity()
        );

        try {

            Inventory updatedInventory =
                    inventoryRepository.save(inventory);

            return InventoryResponse.from(
                    updatedInventory
            );

        } catch (OptimisticLockingFailureException ex) {

            throw new ConcurrentStockUpdateException(
                    "Inventory was modified by another request. "
                            + "Please retry."
            );
        }
    }


    @Transactional
    public StockOperationResponse reserveStock(
            Long productId,
            Integer quantity) {

        validateQuantity(quantity);

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(
                                () -> new InventoryNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        if (inventory.getAvailableQuantity() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock for product: "
                            + productId
            );
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity()
                        - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        + quantity
        );

        try {

            Inventory updatedInventory =
                    inventoryRepository.save(inventory);

            return StockOperationResponse.builder()
                    .productId(productId)
                    .message("Stock reserved successfully")
                    .availableQuantity(
                            updatedInventory
                                    .getAvailableQuantity()
                    )
                    .reservedQuantity(
                            updatedInventory
                                    .getReservedQuantity()
                    )
                    .build();

        } catch (OptimisticLockingFailureException ex) {

            throw new ConcurrentStockUpdateException(
                    "Stock was modified concurrently. "
                            + "Please retry."
            );
        }
    }


    @Transactional
    public StockOperationResponse releaseStock(
            Long productId,
            Integer quantity) {

        validateQuantity(quantity);

        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(
                                () -> new InventoryNotFoundException(
                                        "Inventory not found for product: "
                                                + productId
                                )
                        );

        if (inventory.getReservedQuantity() < quantity) {

            throw new IllegalArgumentException(
                    "Cannot release more stock than reserved"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity()
                        - quantity
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity()
                        + quantity
        );

        try {

            Inventory updatedInventory =
                    inventoryRepository.save(inventory);

            return StockOperationResponse.builder()
                    .productId(productId)
                    .message("Stock released successfully")
                    .availableQuantity(
                            updatedInventory
                                    .getAvailableQuantity()
                    )
                    .reservedQuantity(
                            updatedInventory
                                    .getReservedQuantity()
                    )
                    .build();

        } catch (OptimisticLockingFailureException ex) {

            throw new ConcurrentStockUpdateException(
                    "Stock was modified concurrently. "
                            + "Please retry."
            );
        }
    }


    private void validateQuantity(Integer quantity) {

        if (quantity == null || quantity <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero"
            );
        }
    }
}