package com.splitwise.web;

import com.splitwise.service.TripService;
import com.splitwise.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService service;

    @GetMapping
    public List<TripListItem> list() {
        return service.listTrips();
    }

    @PostMapping
    public TripSummary create(@Valid @RequestBody TripRequest req) {
        return service.createTrip(req);
    }

    @GetMapping("/{id}")
    public TripSummary get(@PathVariable Long id) {
        return service.summary(id);
    }

    @PostMapping("/{id}/members")
    public TripSummary addMember(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        return service.addMember(id, req.name());
    }

    @PostMapping("/{id}/expenses")
    public TripSummary addExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest req) {
        return service.addExpense(id, req);
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    public TripSummary deleteExpense(@PathVariable Long id, @PathVariable Long expenseId) {
        return service.deleteExpense(id, expenseId);
    }

    @PostMapping("/{id}/settlements")
    public TripSummary settle(@PathVariable Long id, @Valid @RequestBody SettleRequest req) {
        return service.settle(id, req);
    }
}
