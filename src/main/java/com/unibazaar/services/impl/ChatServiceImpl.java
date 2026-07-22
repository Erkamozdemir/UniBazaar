package com.unibazaar.services.impl;

import com.unibazaar.daos.ChatDAO;
import com.unibazaar.dtos.ChatDTO;
import com.unibazaar.dtos.MessageDTO;
import com.unibazaar.repositories.ChatRepository;
import com.unibazaar.services.IChatService;

import java.util.List;
import java.util.Optional;

public class ChatServiceImpl implements IChatService {

    private final ChatRepository chatRepository;

    public ChatServiceImpl() {
        this.chatRepository = new ChatDAO();
    }

    @Override
    public List<ChatDTO> getChatsForUser(String userId) {
        return chatRepository.findChatsForUser(userId);
    }

    @Override
    public List<MessageDTO> getMessagesForChat(int chatId) {
        return chatRepository.findMessagesByChatId(chatId);
    }

    @Override
    public void sendMessage(int chatId, String senderId, String content) {
        chatRepository.sendMessage(chatId, senderId, content);
    }

    @Override
    public Optional<ChatDTO> startOrGetChat(int listingId, String buyerId, String sellerId) {
        return chatRepository.findOrCreateChat(listingId, buyerId, sellerId);
    }

    @Override
    public void closeListingChats(int listingId, Integer excludeChatId) {
        chatRepository.closeListingChats(listingId, excludeChatId);
    }
}
