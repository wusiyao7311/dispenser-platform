package com.vtecdemo.dispenser.service;

import com.vtecdemo.dispenser.dto.ProductDto;
import com.vtecdemo.dispenser.model.Product;
import com.vtecdemo.dispenser.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> findAll() {
        return productRepository.findAll().stream().map(this::toDto).toList();
    }

    public ProductDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public ProductDto create(ProductDto dto) {
        if (productRepository.existsBySku(dto.sku())) {
            throw new DuplicateResourceException("Product with SKU '" + dto.sku() + "' already exists");
        }
        Product saved = productRepository.save(new Product(dto.sku(), dto.name(), dto.unitPrice()));
        return toDto(saved);
    }

    public ProductDto update(Long id, ProductDto dto) {
        Product product = getOrThrow(id);
        product.setName(dto.name());
        product.setUnitPrice(dto.unitPrice());
        return toDto(productRepository.save(product));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product " + id + " not found");
        }
        productRepository.deleteById(id);
    }

    Product getOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getUnitPrice());
    }
}
