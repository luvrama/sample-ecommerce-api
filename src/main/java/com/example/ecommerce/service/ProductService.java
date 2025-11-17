package com.example.ecommerce.service;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private EventPublisher eventPublisher;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        Object cached = cacheService.getProduct(id);
        if (cached != null) {
            return (Product) cached;
        }

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        cacheService.cacheProduct(id, product);
        return product;
    }

    public Product createProduct(Product product) {
        Product saved = productRepository.save(product);
        eventPublisher.publishProductCreated(saved.getId(), saved.getName());
        return saved;
    }

    public Product updateProduct(Long id, Product product) {
        Product existing = getProductById(id);
        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        existing.setStock(product.getStock());
        Product updated = productRepository.save(existing);
        cacheService.invalidateProduct(id);
        return updated;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
        cacheService.invalidateProduct(id);
    }
}
