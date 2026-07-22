package com.unibazaar.daos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.ChatDTO;
import com.unibazaar.dtos.MessageDTO;
import com.unibazaar.repositories.ChatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatDAO implements ChatRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final Gson gson = new Gson();

    @Override
    public List<ChatDTO> findChatsForUser(String userId) {
        
        String url = "chats?or=(buyer_id.eq." + userId + ",seller_id.eq." + userId + ")&select=id,listing_id,buyer_id,seller_id,created_at,listings(title,image_url),buyer:users!buyer_id(name),seller:users!seller_id(name)";
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        List<ChatDTO> chats = new ArrayList<>();
        if (array == null) return chats;

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            
            String buyerId = obj.get("buyer_id").getAsString();
            String sellerId = obj.get("seller_id").getAsString();
            boolean isBuyer = userId.equals(buyerId);
            
            String otherParticipantId = isBuyer ? sellerId : buyerId;
            
            String otherParticipantName = "Unknown";
            if (isBuyer && obj.has("seller") && !obj.get("seller").isJsonNull()) {
                otherParticipantName = obj.get("seller").getAsJsonObject().get("name").getAsString();
            } else if (!isBuyer && obj.has("buyer") && !obj.get("buyer").isJsonNull()) {
                otherParticipantName = obj.get("buyer").getAsJsonObject().get("name").getAsString();
            }
            
            String listingTitle = "Unknown Listing";
            String listingImageUrl = null;
            if (obj.has("listings") && !obj.get("listings").isJsonNull()) {
                JsonObject listing = obj.get("listings").getAsJsonObject();
                listingTitle = listing.has("title") && !listing.get("title").isJsonNull() ? listing.get("title").getAsString() : "Unknown";
                listingImageUrl = listing.has("image_url") && !listing.get("image_url").isJsonNull() ? listing.get("image_url").getAsString() : null;
            }

            LocalDateTime createdAt = LocalDateTime.parse(obj.get("created_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME);

            chats.add(new ChatDTO(
                obj.get("id").getAsInt(),
                obj.get("listing_id").getAsInt(),
                listingTitle,
                listingImageUrl,
                otherParticipantId,
                otherParticipantName,
                createdAt
            ));
        }
        return chats;
    }

    @Override
    public List<MessageDTO> findMessagesByChatId(int chatId) {
        String url = "messages?chat_id=eq." + chatId + "&order=created_at.asc";
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        List<MessageDTO> messages = new ArrayList<>();
        if (array == null) return messages;

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            messages.add(new MessageDTO(
                obj.get("id").getAsInt(),
                obj.get("chat_id").getAsInt(),
                obj.get("sender_id").getAsString(),
                obj.get("content").getAsString(),
                LocalDateTime.parse(obj.get("created_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME)
            ));
        }
        return messages;
    }

    @Override
    public void sendMessage(int chatId, String senderId, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("chat_id", chatId);
        obj.addProperty("sender_id", senderId);
        obj.addProperty("content", content);
        client.post("messages", gson.toJson(obj));
    }

    @Override
    public Optional<ChatDTO> findOrCreateChat(int listingId, String buyerId, String sellerId) {
        
        String url = "chats?listing_id=eq." + listingId + "&buyer_id=eq." + buyerId;
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        if (array != null && array.size() > 0) {
            
            
            int chatId = array.get(0).getAsJsonObject().get("id").getAsInt();
            return findChatsForUser(buyerId).stream().filter(c -> c.id() == chatId).findFirst();
        }

        
        JsonObject obj = new JsonObject();
        obj.addProperty("listing_id", listingId);
        obj.addProperty("buyer_id", buyerId);
        obj.addProperty("seller_id", sellerId);
        String createdResponse = client.post("chats", gson.toJson(obj));
        JsonArray createdArray = gson.fromJson(createdResponse, JsonArray.class);
        if (createdArray != null && createdArray.size() > 0) {
            int newChatId = createdArray.get(0).getAsJsonObject().get("id").getAsInt();
            return findChatsForUser(buyerId).stream().filter(c -> c.id() == newChatId).findFirst();
        }
        return Optional.empty();
    }

    public void closeListingChats(int listingId, Integer excludeChatId) {
        String url = "chats?listing_id=eq." + listingId;
        if (excludeChatId != null) {
            url += "&id=neq." + excludeChatId;
        }
        client.patch(url, "{\"status\": \"CLOSED\"}");
    }
}
