package com.unibazaar.services;

import com.unibazaar.dtos.ChatDTO;
import com.unibazaar.dtos.MessageDTO;

import java.util.List;
import java.util.Optional;

public interface IChatService {
    List<ChatDTO> getChatsForUser(String userId);
    List<MessageDTO> getMessagesForChat(int chatId);
    void sendMessage(int chatId, String senderId, String content);
    Optional<ChatDTO> startOrGetChat(int listingId, String buyerId, String sellerId);
    void closeListingChats(int listingId, Integer excludeChatId);
}
