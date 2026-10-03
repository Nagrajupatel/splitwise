package com.splitwise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** A real-world payment: fromMember paid toMember to clear some debt. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member fromMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member toMember;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private Instant createdAt = Instant.now();
}
