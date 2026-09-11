package com.orderflow.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "processed_event",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_processed_event",
            columnNames = {"event_id", "consumer_name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String consumerName;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}