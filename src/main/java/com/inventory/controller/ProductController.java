package com.inventory.controller;

import com.inventory.dto.ProductRequestDto;
import com.inventory.dto.ProductResponseDto;
import com.inventory.exception.DuplicateSkuException;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> response = productRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(this::mapToResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        if (productRepository.existsBySku(requestDto.getSku())) {
            throw new DuplicateSkuException("Product with SKU '" + requestDto.getSku() + "' already exists.");
        }

        Product product = Product.builder()
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        Product savedProduct = productRepository.save(product);
        return new ResponseEntity<>(mapToResponseDto(savedProduct), HttpStatus.CREATED);
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .inStock(product.getStockQuantity() != null && product.getStockQuantity() > 0)
                .build();
    }
}
