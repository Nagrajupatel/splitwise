package com.splitwise.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {
    }

    // ----- requests -----
    public record TripRequest(@NotBlank String name, List<String> members) {
    }

    public record MemberRequest(@NotBlank String name) {
    }

    /** splitType: EQUAL (default), PERCENT or EXACT. */
    public record ExpenseRequest(
            @NotBlank String description,
            String category,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull Long paidBy,
            String splitType,
            List<Long> participants,          // for EQUAL (empty = everyone)
            Map<Long, BigDecimal> values) {   // for PERCENT / EXACT: memberId -> value
    }

    public record SettleRequest(
            @NotNull Long from,
            @NotNull Long to,
            @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    // ----- responses -----
    public record TripListItem(Long id, String name, int memberCount) {
    }

    public record MemberDto(Long id, String name) {
    }

    public record ExpenseDto(Long id, String description, String category, BigDecimal amount,
                             Long paidBy, String paidByName, Instant createdAt,
                             Map<Long, BigDecimal> shares) {
    }

    public record BalanceDto(Long memberId, String name, BigDecimal net) {
    }

    public record TransferDto(Long from, String fromName, Long to, String toName, BigDecimal amount) {
    }

    public record TripSummary(Long id, String name, List<MemberDto> members, BigDecimal total,
                              Map<String, BigDecimal> byCategory, List<BalanceDto> balances,
                              List<TransferDto> settlements, List<ExpenseDto> expenses) {
    }

    public record OcrResult(String merchant, BigDecimal total, String rawText) {
    }
}
