package com.splitwise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Trip trip;

    @Column(nullable = false)
    private String description;

    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Member paidBy;

    private Instant createdAt = Instant.now();

    /** memberId -> how much of this expense that member owes */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "member_id")
    @Column(name = "share", precision = 12, scale = 2)
    private Map<Long, BigDecimal> shares = new LinkedHashMap<>();
}
