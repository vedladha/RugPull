package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.ItemUpdateRequest;
import com.example.demo.model.Item;
import com.example.demo.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemController itemController;

    @Test
    void updateItem_returnsUpdatedItem_whenRequestIsValid() {
        Item existing = buildItem(1, 7, "Old Name", "Old Description", new BigDecimal("1.00"), 1, false);
        ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);

        when(itemRepository.findByItemIdAndDeletedFalse(1)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = itemController.updateItem(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Item saved = (Item) body.get("item");

        assertNotNull(saved);
        assertEquals(1, saved.getItemId());
        assertEquals(7, saved.getUserId());
        assertEquals("Updated Item", saved.getName());
        assertEquals("Updated description", saved.getDescription());
        assertEquals(0, new BigDecimal("49.95").compareTo(saved.getPrice()));
        assertEquals(12, saved.getStock());

        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void updateItem_returnsNotFound_whenItemDoesNotExist() {
        ItemUpdateRequest request = buildRequest("Updated Item", "Updated description", "49.95", 12);
        when(itemRepository.findByItemIdAndDeletedFalse(99)).thenReturn(Optional.empty());

        ResponseEntity<?> response = itemController.updateItem(99, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Item not found", body.get("error"));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_returnsBadRequest_whenValidationFails() {
        Item existing = buildItem(5, 8, "Name", "Description", new BigDecimal("2.00"), 3, false);
        ItemUpdateRequest request = buildRequest("   ", "Updated description", "-1", -2);
        when(itemRepository.findByItemIdAndDeletedFalse(5)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = itemController.updateItem(5, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("name is required", body.get("error"));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_trimsStringFields_beforeSave() {
        Item existing = buildItem(6, 9, "Name", "Description", new BigDecimal("10.00"), 2, false);
        ItemUpdateRequest request = buildRequest("  Trim Me  ", "  Keep Tight  ", "1.00", 1);

        when(itemRepository.findByItemIdAndDeletedFalse(6)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = itemController.updateItem(6, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Item saved = (Item) body.get("item");
        assertNotNull(saved);
        assertEquals("Trim Me", saved.getName());
        assertEquals("Keep Tight", saved.getDescription());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void updateItem_returnsBadRequest_whenRequestBodyMissing() {
        Item existing = buildItem(10, 4, "Name", "Description", new BigDecimal("3.50"), 1, false);
        when(itemRepository.findByItemIdAndDeletedFalse(10)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = itemController.updateItem(10, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertTrue(String.valueOf(body.get("error")).contains("required"));
        verify(itemRepository, never()).save(any(Item.class));
    }

    private ItemUpdateRequest buildRequest(String name, String description, String price, Integer stock) {
        ItemUpdateRequest request = new ItemUpdateRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(new BigDecimal(price));
        request.setStock(stock);
        return request;
    }

    private Item buildItem(Integer itemId, Integer userId, String name, String description,
                           BigDecimal price, Integer stock, Boolean deleted) {
        Item item = new Item();
        item.setItemId(itemId);
        item.setUserId(userId);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setStock(stock);
        item.setDeleted(deleted);
        return item;
    }
}
