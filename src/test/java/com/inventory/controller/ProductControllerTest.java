package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.ProductRequestDto;
import com.inventory.exception.DuplicateSkuException;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductRepository productRepository;

    @Test
    @DisplayName("POST /api/v1/products - Success (201 Created)")
    void createProduct_Success() throws Exception {
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sku("SKU-100")
                .name("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .build();

        Product savedProduct = Product.builder()
                .id(1L)
                .sku("SKU-100")
                .name("Wireless Mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(50)
                .build();

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/products - Duplicate SKU returns 409 Conflict")
    void createProduct_DuplicateSku_Returns409() throws Exception {
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sku("DUPLICATE-SKU")
                .name("Mechanical Keyboard")
                .price(new BigDecimal("89.99"))
                .stockQuantity(10)
                .build();

        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Multiple validation failures merge error messages (400 Bad Request)")
    void createProduct_ValidationFailure_MergesErrorMessages() throws Exception {
        // Name is short (< 2 chars) AND price is missing
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sku("SKU-999")
                .name("A") // Fails @Size(min = 2)
                .price(null) // Fails @NotNull
                .stockQuantity(-5) // Fails @PositiveOrZero
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stockQuantity").exists());
    }

    @Test
    @DisplayName("POST /api/v1/products - Padded input is trimmed before validation runs")
    void createProduct_TrimsInputInSetters() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("  SKU-TRIMMED  ");
        requestDto.setName("  Gaming Headset  ");
        requestDto.setPrice(new BigDecimal("49.99"));
        requestDto.setStockQuantity(20);

        Product savedProduct = Product.builder()
                .id(2L)
                .sku("SKU-TRIMMED")
                .name("Gaming Headset")
                .price(new BigDecimal("49.99"))
                .stockQuantity(20)
                .build();

        when(productRepository.existsBySku("SKU-TRIMMED")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-TRIMMED"))
                .andExpect(jsonPath("$.name").value("Gaming Headset"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Existing ID returns 200 OK")
    void getProductById_Success() throws Exception {
        Product product = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Monitor")
                .price(new BigDecimal("199.99"))
                .stockQuantity(15)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Non-existent ID returns 404 Not Found")
    void getProductById_NotFound() throws Exception {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }
}
