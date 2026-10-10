package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.ProductRequestDto;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    @DisplayName("POST /api/v1/products - Success captures entity and verifies all fields")
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
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.stockQuantity").value(50))
                .andExpect(jsonPath("$.inStock").value(true));

        // Verify captured arguments sent to database layer
        verify(productRepository).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();

        assertThat(capturedProduct.getSku(), is("SKU-100"));
        assertThat(capturedProduct.getName(), is("Wireless Mouse"));
        assertThat(capturedProduct.getPrice(), is(new BigDecimal("29.99")));
        assertThat(capturedProduct.getStockQuantity(), is(50));
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
    @DisplayName("POST /api/v1/products - Multiple validation failures on single field merge error messages")
    void createProduct_ValidationFailure_MergesErrorMessages() throws Exception {
        // Name is " " which fails both @NotBlank and @Size(min = 2)
        String rawJson = """
                {
                    "sku": "SKU-999",
                    "name": " ",
                    "price": 19.99,
                    "stockQuantity": 10
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name", containsString("; ")))
                .andExpect(jsonPath("$.errors.name", containsString("Product name is required")))
                .andExpect(jsonPath("$.errors.name", containsString("Product name must be between 2 and 100 characters")));
    }

    @Test
    @DisplayName("POST /api/v1/products - Raw JSON with whitespace is trimmed before repository save")
    void createProduct_TrimsInputInSetters() throws Exception {
        String rawJsonWithPadding = """
                {
                    "sku": "  SKU-TRIMMED  ",
                    "name": "  Gaming Headset  ",
                    "price": 49.99,
                    "stockQuantity": 20
                }
                """;

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
                        .content(rawJsonWithPadding))
                .andExpect(status().isCreated());

        // Verify the entity saved to the repository has trimmed fields
        verify(productRepository).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();

        assertThat(capturedProduct.getSku(), is("SKU-TRIMMED"));
        assertThat(capturedProduct.getName(), is("Gaming Headset"));
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
