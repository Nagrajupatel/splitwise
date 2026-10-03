package com.splitwise.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrServiceTest {

    @Test
    void picksLabelledTotalOverSubtotal() {
        String bill = "Spice Garden\nPaneer Tikka 250.00\nNaan 60.00\nSubtotal 310.00\nGST 15.50\nTOTAL 325.50\n";
        assertEquals(new BigDecimal("325.50"), OcrService.guessTotal(bill));
        assertEquals("Spice Garden", OcrService.guessMerchant(bill));
    }

    @Test
    void fallsBackToLargestAmount() {
        assertEquals(new BigDecimal("1200.00"), OcrService.guessTotal("Cafe\nCoffee 150.00\nCake 1,200.00\n"));
    }
}
