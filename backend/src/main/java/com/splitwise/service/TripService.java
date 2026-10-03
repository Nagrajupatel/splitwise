package com.splitwise.service;

import com.splitwise.model.*;
import com.splitwise.repo.*;
import com.splitwise.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {
    private final TripRepository trips;
    private final MemberRepository members;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    @Transactional(readOnly = true)
    public List<TripListItem> listTrips() {
        return trips.findAll().stream()
                .map(t -> new TripListItem(t.getId(), t.getName(), t.getMembers().size()))
                .toList();
    }

    @Transactional
    public TripSummary createTrip(TripRequest req) {
        Trip trip = new Trip();
        trip.setName(req.name().trim());
        trip = trips.save(trip);
        if (req.members() != null) {
            for (String name : req.members()) {
                if (name != null && !name.isBlank()) addMemberTo(trip, name);
            }
        }
        return summary(trip.getId());
    }

    @Transactional
    public TripSummary addMember(Long tripId, String name) {
        addMemberTo(getTrip(tripId), name);
        return summary(tripId);
    }

    @Transactional
    public TripSummary addExpense(Long tripId, ExpenseRequest req) {
        Trip trip = getTrip(tripId);
        Map<Long, Member> byId = trip.getMembers().stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        Member payer = byId.get(req.paidBy());
        if (payer == null) throw new IllegalArgumentException("The payer isn't part of this trip");

        BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
        String type = req.splitType() == null ? "EQUAL" : req.splitType().toUpperCase();

        Map<Long, BigDecimal> shares = switch (type) {
            case "EQUAL" -> SplitCalculator.equal(amount, participants(req.participants(), byId));
            case "PERCENT" -> SplitCalculator.percentage(amount, onlyTripMembers(req.values(), byId));
            case "EXACT" -> SplitCalculator.exact(amount, onlyTripMembers(req.values(), byId));
            default -> throw new IllegalArgumentException("Unknown split type: " + type);
        };

        Expense e = new Expense();
        e.setTrip(trip);
        e.setDescription(req.description().trim());
        e.setCategory(req.category() == null || req.category().isBlank() ? "Other" : req.category().trim());
        e.setAmount(amount);
        e.setPaidBy(payer);
        e.setShares(new LinkedHashMap<>(shares));
        expenses.save(e);
        return summary(tripId);
    }

    @Transactional
    public TripSummary deleteExpense(Long tripId, Long expenseId) {
        Expense e = expenses.findById(expenseId)
                .filter(x -> x.getTrip().getId().equals(tripId))
                .orElseThrow(() -> new NoSuchElementException("Expense not found"));
        expenses.delete(e);
        return summary(tripId);
    }

    @Transactional
    public TripSummary settle(Long tripId, SettleRequest req) {
        Trip trip = getTrip(tripId);
        Map<Long, Member> byId = trip.getMembers().stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
        Member from = byId.get(req.from());
        Member to = byId.get(req.to());
        if (from == null || to == null) throw new IllegalArgumentException("Both people must be part of this trip");
        if (from.getId().equals(to.getId())) throw new IllegalArgumentException("Can't settle with yourself");

        Settlement s = new Settlement();
        s.setTrip(trip);
        s.setFromMember(from);
        s.setToMember(to);
        s.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        settlements.save(s);
        return summary(tripId);
    }

    /** Everything the trip screen needs: totals, category split, who owes whom. */
    @Transactional(readOnly = true)
    public TripSummary summary(Long tripId) {
        Trip trip = getTrip(tripId);
        List<Expense> list = expenses.findByTripIdOrderByCreatedAtDesc(tripId);

        Map<Long, String> names = new LinkedHashMap<>();
        trip.getMembers().forEach(m -> names.put(m.getId(), m.getName()));

        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        names.keySet().forEach(id -> net.put(id, BigDecimal.ZERO));

        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new TreeMap<>();

        for (Expense e : list) {
            total = total.add(e.getAmount());
            byCategory.merge(e.getCategory() == null ? "Other" : e.getCategory(), e.getAmount(), BigDecimal::add);
            net.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add);          // paid -> is owed
            e.getShares().forEach((id, share) -> net.merge(id, share.negate(), BigDecimal::add)); // consumed -> owes
        }
        for (Settlement s : settlements.findByTripId(tripId)) {
            net.merge(s.getFromMember().getId(), s.getAmount(), BigDecimal::add);
            net.merge(s.getToMember().getId(), s.getAmount().negate(), BigDecimal::add);
        }

        List<BalanceDto> balances = net.entrySet().stream()
                .map(en -> new BalanceDto(en.getKey(), names.get(en.getKey()), en.getValue()))
                .toList();

        List<TransferDto> transfers = SplitCalculator.simplify(net).stream()
                .map(t -> new TransferDto(t.from(), names.get(t.from()), t.to(), names.get(t.to()), t.amount()))
                .toList();

        List<ExpenseDto> expenseDtos = list.stream()
                .map(e -> new ExpenseDto(e.getId(), e.getDescription(), e.getCategory(), e.getAmount(),
                        e.getPaidBy().getId(), e.getPaidBy().getName(), e.getCreatedAt(), e.getShares()))
                .toList();

        List<MemberDto> memberDtos = names.entrySet().stream()
                .map(en -> new MemberDto(en.getKey(), en.getValue()))
                .toList();

        return new TripSummary(trip.getId(), trip.getName(), memberDtos, total, byCategory,
                balances, transfers, expenseDtos);
    }

    // ----- helpers -----
    private Trip getTrip(Long id) {
        return trips.findById(id).orElseThrow(() -> new NoSuchElementException("Trip not found"));
    }

    private void addMemberTo(Trip trip, String name) {
        Member m = new Member();
        m.setName(name.trim());
        m.setTrip(trip);
        members.save(m);
        trip.getMembers().add(m);
    }

    private List<Long> participants(List<Long> ids, Map<Long, Member> byId) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>(byId.keySet());
        for (Long id : ids) {
            if (!byId.containsKey(id)) throw new IllegalArgumentException("Someone in the split isn't part of this trip");
        }
        return ids;
    }

    private Map<Long, BigDecimal> onlyTripMembers(Map<Long, BigDecimal> values, Map<Long, Member> byId) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Enter a value for each person");
        for (Long id : values.keySet()) {
            if (!byId.containsKey(id)) throw new IllegalArgumentException("Someone in the split isn't part of this trip");
        }
        return values;
    }
}
