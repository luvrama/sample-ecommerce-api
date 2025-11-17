package com.example.ecommerce.service;

import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private EventPublisher eventPublisher;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order createOrder(Order order) {
        Product product = productService.getProductById(order.getProductId());
        
        if (product.getStock() < order.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        BigDecimal total = product.getPrice().multiply(new BigDecimal(order.getQuantity()));
        order.setTotalAmount(total);

        boolean paymentSuccess = paymentClient.processPayment(order.getCustomerEmail(), total);
        if (!paymentSuccess) {
            throw new RuntimeException("Payment failed");
        }

        product.setStock(product.getStock() - order.getQuantity());
        productService.updateProduct(product.getId(), product);

        Order saved = orderRepository.save(order);
        eventPublisher.publishOrderCreated(saved.getId(), saved.getCustomerEmail());
        
        return saved;
    }
}
