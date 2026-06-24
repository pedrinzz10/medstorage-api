package com.saas.MedStorage_api.inventory;

import com.saas.MedStorage_api.inventory.dto.InventoryStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/status")
    public ResponseEntity<List<InventoryStatusResponse>> getStatus() {
        return ResponseEntity.ok(inventoryService.getStatus());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryStatusResponse> findByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.findByProductId(productId));
    }
}
