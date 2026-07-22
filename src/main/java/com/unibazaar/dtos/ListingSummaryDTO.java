package com.unibazaar.dtos;
import java.math.BigDecimal;
public record ListingSummaryDTO(
    int id,
    String title,
    BigDecimal originalPrice,
    boolean isCampaignActive,
    String imageUrl,
    String courseCode,
    String categoryName,
    String brand,
    String status,
    String targetUniversity
) {}
