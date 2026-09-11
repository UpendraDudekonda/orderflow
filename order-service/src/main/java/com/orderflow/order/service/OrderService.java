package com.orderflow.order.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.order.client.ProductServiceClient;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderItemRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.dto.ProductResponse;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderItem;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.event.InventoryReleaseRequestedEvent;
import com.orderflow.order.event.OrderCreatedEvent;
import com.orderflow.order.event.PaymentRequestedEvent;
import com.orderflow.order.exception.OrderNotFoundException;
import com.orderflow.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final ProductServiceClient productServiceClient;
    
    private final OutboxService outboxService;

    @Transactional
    public OrderResponse createOrder(
            Long userId,
            CreateOrderRequest request) {
    	
    	String paymentMethod =
    	        request.getPaymentMethod();

    	if (paymentMethod == null ||
    	        paymentMethod.isBlank()) {

    	    paymentMethod = "CARD";
    	}

    	Order order =
    	        Order.builder()
    	                .userId(userId)
    	                .status(OrderStatus.CREATED)
    	                .currency("INR")
    	                .paymentMethod(
    	                        paymentMethod
    	                )
    	                .totalAmount(BigDecimal.ZERO)
    	                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {

            ProductResponse product =
            		productServiceClient.getProductById(
                            itemRequest.getProductId()
                    );

            if (product == null || Boolean.FALSE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is not available: "
                                + itemRequest.getProductId()
                );
            }

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    itemRequest.getQuantity()
                            )
                    );

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .order(order)
                    .build();

            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder =
                orderRepository.save(order);
        //create event -> order.created with eventid and orderid
        OrderCreatedEvent event =
                OrderCreatedEvent.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .orderId(savedOrder.getId())
                        .userId(savedOrder.getUserId())
                        .totalAmount(savedOrder.getTotalAmount())
                        .currency(savedOrder.getCurrency())
                        .occurredAt(java.time.LocalDateTime.now())
                        .items(
                                savedOrder.getItems()
                                        .stream()
                                        .map(item ->
                                                OrderCreatedEvent.OrderItemEvent
                                                        .builder()
                                                        .productId(
                                                                item.getProductId()
                                                        )
                                                        .quantity(
                                                                item.getQuantity()
                                                        )
                                                        .unitPrice(
                                                                item.getUnitPrice()
                                                        )
                                                        .build()
                                        )
                                        .toList()
                        )
                        .build();

        outboxService.saveEvent(
                event.getEventId(),
                "Order",
                String.valueOf(savedOrder.getId()),
                "OrderCreatedEvent",
                "order.created",
                event
        );

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(
            Long orderId,
            Long userId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to access this order"
            );
        }

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(
            Long userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(
            Long orderId,
            Long userId) {

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found with id: "
                                                + orderId
                                )
                        );

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to cancel this order"
            );
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Order is already cancelled"
            );
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Confirmed order cannot be cancelled"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(
                orderRepository.save(order)
        );
    }
    
    @Transactional
    public void handleInventoryReserved(
            Long orderId) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        if (order.getStatus()
                != OrderStatus.CREATED) {

            log.warn(
                    "Ignoring inventory reserved event for order {} because current status is {}",
                    orderId,
                    order.getStatus()
            );

            return;
        }

        /*
         * Inventory reservation succeeded.
         */
        order.setStatus(
                OrderStatus.PAYMENT_PENDING
        );

        orderRepository.save(order);

        /*
         * Generate idempotency key.
         *
         * Same order should not create
         * multiple payment requests.
         */
        String idempotencyKey =
                "ORDER-" + order.getId();

        PaymentRequestedEvent event =
                PaymentRequestedEvent.builder()
                        .eventId(
                                java.util.UUID.randomUUID()
                                        .toString()
                        )
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .amount(order.getTotalAmount())
                        .currency(order.getCurrency())
                        .idempotencyKey(
                                "ORDER-" + order.getId()
                        )
                        .paymentMethod(
                                order.getPaymentMethod()
                        )
                        .occurredAt(
                                java.time.LocalDateTime.now()
                        )
                        .build();

        outboxService.saveEvent(
                event.getEventId(),
                "Order",
                String.valueOf(order.getId()),
                "PaymentRequestedEvent",
                "payment.requested",
                event
        );

        log.info(
                "PaymentRequestedEvent published for order {}",
                orderId
        );
    }
    
    @Transactional
    public void handleInventoryFailed(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found: " + orderId
                        ));

        if (order.getStatus() != OrderStatus.CREATED) {

            log.warn(
                    "Ignoring inventory failed event for order {} because current status is {}",
                    orderId,
                    order.getStatus()
            );

            return;
        }

        order.setStatus(OrderStatus.INVENTORY_FAILED);

        orderRepository.save(order);

        log.info(
                "Order {} status updated to INVENTORY_FAILED",
                orderId
        );
    }
    
    @Transactional
    public void handlePaymentSucceeded(
            Long orderId) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        if (order.getStatus()
                != OrderStatus.PAYMENT_PENDING) {

            log.warn(
                    "Ignoring payment success for order {} because current status is {}",
                    orderId,
                    order.getStatus()
            );

            return;
        }

        order.setStatus(
                OrderStatus.CONFIRMED
        );

        orderRepository.save(order);

        log.info(
                "Order {} confirmed after successful payment",
                orderId
        );
    }
    
    @Transactional
    public void handlePaymentFailed(
            Long orderId) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        if (order.getStatus()
                != OrderStatus.PAYMENT_PENDING) {

            log.warn(
                    "Ignoring payment failure for order {} because current status is {}",
                    orderId,
                    order.getStatus()
            );

            return;
        }

        order.setStatus(
                OrderStatus.PAYMENT_FAILED
        );

        orderRepository.save(order);

        /*
         * Request inventory compensation.
         */

        for (OrderItem item : order.getItems()) {

            InventoryReleaseRequestedEvent event =
                    InventoryReleaseRequestedEvent
                            .builder()
                            .eventId(
                                    java.util.UUID
                                            .randomUUID()
                                            .toString()
                            )
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .occurredAt(
                                    java.time.LocalDateTime.now()
                            )
                            .build();

            outboxService.saveEvent(
                    event.getEventId(),
                    "Order",
                    String.valueOf(order.getId()),
                    "InventoryReleaseRequestedEvent",
                    "inventory.release.requested",
                    event
            );
        }

        log.warn(
                "Payment failed. Inventory release requested for order {}",
                orderId
        );
    }
    
    @Transactional
    public void handleInventoryReleased(
            Long orderId) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new OrderNotFoundException(
                                        "Order not found: "
                                                + orderId
                                )
                        );

        if (order.getStatus()
                != OrderStatus.PAYMENT_FAILED) {

            log.warn(
                    "Ignoring inventory released event for order {} because current status is {}",
                    orderId,
                    order.getStatus()
            );

            return;
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );

        orderRepository.save(order);

        log.info(
                "Order {} cancelled after inventory compensation",
                orderId
        );
    }
}