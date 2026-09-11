package com.orderflow.inventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflow.inventory.dto.CreateInventoryRequest;
import com.orderflow.inventory.dto.InventoryResponse;
import com.orderflow.inventory.dto.StockOperationResponse;
import com.orderflow.inventory.dto.UpdateInventoryRequest;
import com.orderflow.inventory.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        inventoryService.createInventory(
                                request
                        )
                );
    }


    @GetMapping
    public ResponseEntity<List<InventoryResponse>>
    getAllInventory() {

        return ResponseEntity.ok(
                inventoryService.getAllInventory()
        );
    }


    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse>
    getInventory(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                inventoryService
                        .getInventoryByProductId(
                                productId
                        )
        );
    }


    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse>
    updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody
            UpdateInventoryRequest request) {

        return ResponseEntity.ok(
                inventoryService.updateInventory(
                        productId,
                        request
                )
        );
    }


    @PostMapping("/{productId}/reserve")
    public ResponseEntity<StockOperationResponse>
    reserveStock(
            @PathVariable Long productId,
            @RequestBody Integer quantity) {

        return ResponseEntity.ok(
                inventoryService.reserveStock(
                        productId,
                        quantity
                )
        );
    }


    @PostMapping("/{productId}/release")
    public ResponseEntity<StockOperationResponse>
    releaseStock(
            @PathVariable Long productId,
            @RequestBody Integer quantity) {

        return ResponseEntity.ok(
                inventoryService.releaseStock(
                        productId,
                        quantity
                )
        );
    }
}