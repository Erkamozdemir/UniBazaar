package com.unibazaar.repositories;

import com.unibazaar.dtos.ChatDTO;
import com.unibazaar.dtos.MessageDTO;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    List<ChatDTO> findChatsForUser(String userId);
    List<MessageDTO> findMessagesByChatId(int chatId);
    void sendMessage(int chatId, String senderId, String content);
    Optional<ChatDTO> findOrCreateChat(int listingId, String buyerId, String sellerId);
    void closeListingChats(int listingId, Integer excludeChatId);
}
