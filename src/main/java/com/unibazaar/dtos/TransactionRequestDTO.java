package com.unibazaar.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequestDTO(
    int listingId,
    String buyerId,
    BigDecimal finalPrice,
    LocalDateTime transactionDate
) {}
