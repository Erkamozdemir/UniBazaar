package com.unibazaar.dtos;

import java.time.LocalDateTime;

public record MessageDTO(
    int id,
    int chatId,
    String senderId,
    String content,
    LocalDateTime createdAt
) {}
