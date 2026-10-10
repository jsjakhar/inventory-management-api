package com.inventory.service;

import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.exception.DuplicateSkuException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import com.inventory.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    @DisplayName("createProduct - Success saves product and maps response correctly")
    void createProduct_Success() {
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sku("SKU-001")
                .name("Mechanical Keyboard")
                .price(new BigDecimal("99.99"))
                .stockQuantity(15)
                .build();

        Product savedProduct = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Mechanical Keyboard")
                .price(new BigDecimal("99.99"))
                .stockQuantity(15)
                .build();

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponseDto result = productService.createProduct(requestDto);

        assertThat(result.getId(), is(1L));
        assertThat(result.getSku(), is("SKU-001"));
        assertThat(result.getName(), is("Mechanical Keyboard"));
        assertThat(result.getPrice(), is(new BigDecimal("99.99")));
        assertThat(result.getStockQuantity(), is(15));
        assertThat(result.isInStock(), is(true));

        verify(productRepository).save(productCaptor.capture());
        Product captured = productCaptor.getValue();
        assertThat(captured.getSku(), is("SKU-001"));
        assertThat(captured.getName(), is("Mechanical Keyboard"));
        assertThat(captured.getPrice(), is(new BigDecimal("99.99")));
        assertThat(captured.getStockQuantity(), is(15));
    }

    @Test
    @DisplayName("createProduct - Duplicate SKU throws DuplicateSkuException")
    void createProduct_DuplicateSku_ThrowsException() {
        ProductRequestDto requestDto = ProductRequestDto.builder()
                .sku("DUPLICATE-SKU")
                .name("Keyboard")
                .price(new BigDecimal("50.00"))
                .stockQuantity(5)
                .build();

        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        DuplicateSkuException exception = assertThrows(
                DuplicateSkuException.class,
                () -> productService.createProduct(requestDto)
        );

        assertThat(exception.getMessage(), containsString("already exists"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProductById - Success returns response DTO")
    void getProductById_Success() {
        Product product = Product.builder()
                .id(10L)
                .sku("SKU-010")
                .name("USB-C Hub")
                .price(new BigDecimal("35.00"))
                .stockQuantity(0)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponseDto result = productService.getProductById(10L);

        assertThat(result.getId(), is(10L));
        assertThat(result.getSku(), is("SKU-010"));
        assertThat(result.isInStock(), is(false));
    }

    @Test
    @DisplayName("getProductById - Missing ID throws ResourceNotFoundException")
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        assertThat(exception.getMessage(), containsString("Product not found with id: 99"));
    }

    @Test
    @DisplayName("getAllProducts - Returns list of mapped DTOs")
    void getAllProducts_ReturnsList() {
        List<Product> products = List.of(
                Product.builder().id(1L).sku("SKU-1").name("Item 1").price(new BigDecimal("10.00")).stockQuantity(5).build(),
                Product.builder().id(2L).sku("SKU-2").name("Item 2").price(new BigDecimal("20.00")).stockQuantity(0).build()
        );

        when(productRepository.findAll()).thenReturn(products);

        List<ProductResponseDto> result = productService.getAllProducts();

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getSku(), is("SKU-1"));
        assertThat(result.get(0).isInStock(), is(true));
        assertThat(result.get(1).getSku(), is("SKU-2"));
        assertThat(result.get(1).isInStock(), is(false));
    }

    @Test
    @DisplayName("deleteProduct - Missing ID throws ResourceNotFoundException")
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.existsById(55L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.deleteProduct(55L)
        );

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteProduct - Existing ID deletes successfully")
    void deleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }
}
