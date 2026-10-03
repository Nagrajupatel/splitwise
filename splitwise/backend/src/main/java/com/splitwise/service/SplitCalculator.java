package com.splitwise.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * All the money maths lives here. Pure functions, no Spring, no database.
 * Everything is in BigDecimal with 2 decimals so cents never go missing.
 */
public final class SplitCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private SplitCalculator() {
    }

    public record Transfer(Long from, Long to, BigDecimal amount) {
    }

    /** Equal split. Leftover cents go to the first people so the total always matches. */
    public static Map<Long, BigDecimal> equal(BigDecimal total, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one person to split with");
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        int n = memberIds.size();
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        int extraCents = total.subtract(base.multiply(BigDecimal.valueOf(n))).movePointRight(2).intValueExact();

        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            out.put(memberIds.get(i), i < extraCents ? base.add(CENT) : base);
        }
        return out;
    }

    /** Percentage split. Percentages must add up to 100. */
    public static Map<Long, BigDecimal> percentage(BigDecimal total, Map<Long, BigDecimal> percents) {
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = percents.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentages must add up to 100 (currently " + sum.stripTrailingZeros().toPlainString() + ")");
        }
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : percents.entrySet()) {
            if (e.getValue().signum() < 0) throw new IllegalArgumentException("Percentages can't be negative");
            BigDecimal share = total.multiply(e.getValue()).divide(HUNDRED, 2, RoundingMode.DOWN);
            out.put(e.getKey(), share);
            assigned = assigned.add(share);
        }
        int extraCents = total.subtract(assigned).movePointRight(2).intValueExact();
        for (Long id : out.keySet()) {
            if (extraCents-- <= 0) break;
            out.put(id, out.get(id).add(CENT));
        }
        return out;
    }

    /** Exact amounts per person. Must add up to the total. */
    public static Map<Long, BigDecimal> exact(BigDecimal total, Map<Long, BigDecimal> amounts) {
        total = total.setScale(2, RoundingMode.HALF_UP);
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : amounts.entrySet()) {
            if (e.getValue().signum() < 0) throw new IllegalArgumentException("Amounts can't be negative");
            BigDecimal v = e.getValue().setScale(2, RoundingMode.HALF_UP);
            out.put(e.getKey(), v);
            sum = sum.add(v);
        }
        if (sum.compareTo(total) != 0) {
            throw new IllegalArgumentException("Amounts add up to " + sum + " but the bill is " + total);
        }
        return out;
    }

    /**
     * Turns net balances (positive = is owed, negative = owes) into the fewest payments
     * that settle everyone. Greedy: biggest debtor pays biggest creditor.
     */
    public static List<Transfer> simplify(Map<Long, BigDecimal> net) {
        List<Node> creditors = new ArrayList<>();
        List<Node> debtors = new ArrayList<>();
        net.forEach((id, amt) -> {
            if (amt.signum() > 0) creditors.add(new Node(id, amt));
            else if (amt.signum() < 0) debtors.add(new Node(id, amt.negate()));
        });
        Comparator<Node> biggestFirst = (a, b) -> b.amount.compareTo(a.amount);
        creditors.sort(biggestFirst);
        debtors.sort(biggestFirst);

        List<Transfer> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < creditors.size() && j < debtors.size()) {
            Node c = creditors.get(i), d = debtors.get(j);
            BigDecimal pay = c.amount.min(d.amount);
            out.add(new Transfer(d.id, c.id, pay));
            c.amount = c.amount.subtract(pay);
            d.amount = d.amount.subtract(pay);
            if (c.amount.signum() == 0) i++;
            if (d.amount.signum() == 0) j++;
        }
        return out;
    }

    private static final class Node {
        final Long id;
        BigDecimal amount;

        Node(Long id, BigDecimal amount) {
            this.id = id;
            this.amount = amount;
        }
    }
}
