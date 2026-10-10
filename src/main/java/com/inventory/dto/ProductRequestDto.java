package com.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "SKU is required")
    @Size(max = 255, message = "SKU must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "SKU must contain only letters, numbers, hyphens, and underscores")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    public void setSku(String sku) {
        this.sku = sku != null ? sku.trim() : null;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
