package com.unibazaar.dtos;

import java.math.BigDecimal;

public record UserProfileDTO(
    String id,
    String name,
    String email,
    String role,
    BigDecimal gpa,
    boolean isGpaVerified,
    BigDecimal rating,
    int reviewCount
) {}
