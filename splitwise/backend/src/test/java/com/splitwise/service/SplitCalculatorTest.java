package com.splitwise.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    private static BigDecimal sum(Map<Long, BigDecimal> m) {
        return m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void equalSplitSpreadsLeftoverCents() {
        var shares = SplitCalculator.equal(new BigDecimal("100.00"), List.of(1L, 2L, 3L));
        assertEquals(new BigDecimal("33.34"), shares.get(1L));
        assertEquals(new BigDecimal("33.33"), shares.get(2L));
        assertEquals(new BigDecimal("33.33"), shares.get(3L));
        assertEquals(new BigDecimal("100.00"), sum(shares));
    }

    @Test
    void percentSplitAddsUpToTotal() {
        Map<Long, BigDecimal> pct = new LinkedHashMap<>();
        pct.put(1L, new BigDecimal("50"));
        pct.put(2L, new BigDecimal("30"));
        pct.put(3L, new BigDecimal("20"));
        var shares = SplitCalculator.percentage(new BigDecimal("99.99"), pct);
        assertEquals(new BigDecimal("99.99"), sum(shares));
    }

    @Test
    void percentMustTotal100() {
        Map<Long, BigDecimal> pct = Map.of(1L, new BigDecimal("60"), 2L, new BigDecimal("30"));
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.percentage(new BigDecimal("50"), pct));
    }

    @Test
    void exactMustMatchTotal() {
        Map<Long, BigDecimal> amounts = Map.of(1L, new BigDecimal("10"), 2L, new BigDecimal("15"));
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.exact(new BigDecimal("30"), amounts));
    }

    @Test
    void simplifyUsesFewestPayments() {
        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        net.put(1L, new BigDecimal("30.00"));   // is owed 30
        net.put(2L, new BigDecimal("-10.00"));  // owes 10
        net.put(3L, new BigDecimal("-20.00"));  // owes 20
        var transfers = SplitCalculator.simplify(net);

        assertEquals(2, transfers.size());
        assertEquals(new SplitCalculator.Transfer(3L, 1L, new BigDecimal("20.00")), transfers.get(0));
        assertEquals(new SplitCalculator.Transfer(2L, 1L, new BigDecimal("10.00")), transfers.get(1));
    }
}
