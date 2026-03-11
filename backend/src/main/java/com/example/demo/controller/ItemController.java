package com.example.demo.controller;

import com.example.demo.dto.ItemUpdateRequest;
import com.example.demo.model.Item;
import com.example.demo.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable Integer itemId, @RequestBody ItemUpdateRequest request) {
        Optional<Item> existing = itemRepository.findByItemIdAndDeletedFalse(itemId);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Item not found"));
        }

        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        Item item = existing.get();
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription().trim());
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());

        Item saved = itemRepository.save(item);
        return ResponseEntity.ok(Map.of("item", saved));
    }

    private String validate(ItemUpdateRequest request) {
        if (request == null) {
            return "Request body is required";
        }
        if (isBlank(request.getName())) {
            return "name is required";
        }
        if (isBlank(request.getDescription())) {
            return "description is required";
        }
        if (request.getPrice() == null) {
            return "price is required";
        }
        if (request.getStock() == null) {
            return "stock is required";
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return "price must be non-negative";
        }
        if (request.getStock() < 0) {
            return "stock must be non-negative";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
