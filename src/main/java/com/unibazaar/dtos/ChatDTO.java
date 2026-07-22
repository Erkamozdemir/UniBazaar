package com.unibazaar.dtos;

import java.time.LocalDateTime;

public record ChatDTO(
    int id,
    int listingId,
    String listingTitle,
    String listingImageUrl,
    String otherParticipantId,
    String otherParticipantName,
    LocalDateTime createdAt
) {}
