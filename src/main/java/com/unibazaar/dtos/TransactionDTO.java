package com.unibazaar.dtos;

import java.math.BigDecimal;

public record TransactionDTO(
    int id,
    int listingId,
    String buyerId,
    BigDecimal finalPrice,
    String status,
    String transactionDate
) {}
