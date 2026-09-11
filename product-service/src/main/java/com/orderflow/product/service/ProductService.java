package com.orderflow.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.product.dto.ProductRequest;
import com.orderflow.product.dto.ProductResponse;
import com.orderflow.product.entity.Product;
import com.orderflow.product.exception.ProductNotFoundException;
import com.orderflow.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_CACHE_PREFIX =
            "product:";

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory().trim())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        Object cachedProduct =
                redisTemplate.opsForValue().get(cacheKey);

        if (cachedProduct instanceof ProductResponse response) {

            return response;
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        ProductResponse response =
                ProductResponse.from(product);

        redisTemplate.opsForValue()
                .set(cacheKey, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        return productRepository
                .findByActiveTrue(pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            String keyword,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        return productRepository
                .findByActiveTrueAndNameContainingIgnoreCase(
                        keyword.trim(),
                        pageable
                )
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory().trim());

        Product updatedProduct =
                productRepository.save(product);

        evictProductCache(id);

        return ProductResponse.from(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        /*
         * Soft delete.
         *
         * We don't physically remove the product because
         * historical orders may still reference it.
         */
        product.setActive(false);

        productRepository.save(product);

        evictProductCache(id);
    }

    private void evictProductCache(Long id) {

        redisTemplate.delete(
                PRODUCT_CACHE_PREFIX + id
        );
    }
}