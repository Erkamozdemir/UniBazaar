package com.unibazaar.daos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.ListingSummaryDTO;
import com.unibazaar.repositories.ListingRepository;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListingDAO implements ListingRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final Gson gson = new Gson();

    public List<ListingSummaryDTO> findAllActive(String query, Integer categoryId, String brand, BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder url = new StringBuilder("listings?select=id,title,original_price,is_campaign_active,image_url,course_code,brand,status,target_university,categories(name)&status=eq.active");
        
        if (query != null && !query.isBlank()) {
            url.append("&title=ilike.*").append(URLEncoder.encode(query, StandardCharsets.UTF_8)).append("*");
        }
        if (categoryId != null) {
            url.append("&category_id=eq.").append(categoryId);
        }
        if (brand != null && !brand.isBlank()) {
            url.append("&brand=ilike.*").append(URLEncoder.encode(brand, StandardCharsets.UTF_8)).append("*");
        }
        if (minPrice != null) {
            url.append("&original_price=gte.").append(minPrice);
        }
        if (maxPrice != null) {
            url.append("&original_price=lte.").append(maxPrice);
        }

        String response = client.get(url.toString());
        JsonArray array = gson.fromJson(response, JsonArray.class);
        List<ListingSummaryDTO> listings = new ArrayList<>();
        if (array == null) return listings;

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String categoryName = "";
            if (obj.has("categories") && !obj.get("categories").isJsonNull()) {
                categoryName = obj.get("categories").getAsJsonObject().get("name").getAsString();
            }
            listings.add(new ListingSummaryDTO(
                obj.get("id").getAsInt(),
                obj.get("title").getAsString(),
                obj.get("original_price").getAsBigDecimal(),
                obj.get("is_campaign_active").getAsBoolean(),
                obj.get("image_url").isJsonNull() ? null : obj.get("image_url").getAsString(),
                obj.get("course_code").isJsonNull() ? null : obj.get("course_code").getAsString(),
                categoryName,
                obj.get("brand").isJsonNull() ? null : obj.get("brand").getAsString(),
                obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "active",
                obj.has("target_university") && !obj.get("target_university").isJsonNull() ? obj.get("target_university").getAsString() : null
            ));
        }
        return listings;
    }

    public Optional<ListingDetailDTO> findById(int id) {
        String response = client.get("listings?id=eq." + id + "&select=id,seller_id,category_id,title,description,original_price,is_campaign_active,image_url,course_code,brand,condition,location,status,target_university,users!listings_seller_id_fkey(name),categories(name)");
        JsonArray array = gson.fromJson(response, JsonArray.class);
        if (array == null || array.isEmpty()) {
            return Optional.empty();
        }
        JsonObject obj = array.get(0).getAsJsonObject();
        String sellerName = "";
        if (obj.has("users") && !obj.get("users").isJsonNull()) {
            sellerName = obj.get("users").getAsJsonObject().get("name").getAsString();
        }
        String categoryName = "";
        if (obj.has("categories") && !obj.get("categories").isJsonNull()) {
            categoryName = obj.get("categories").getAsJsonObject().get("name").getAsString();
        }
        return Optional.of(new ListingDetailDTO(
            obj.get("id").getAsInt(),
            obj.get("seller_id").getAsString(),
            sellerName,
            obj.get("title").getAsString(),
            obj.get("description").isJsonNull() ? "" : obj.get("description").getAsString(),
            obj.get("original_price").getAsBigDecimal(),
            obj.get("is_campaign_active").getAsBoolean(),
            obj.get("image_url").isJsonNull() ? null : obj.get("image_url").getAsString(),
            obj.get("course_code").isJsonNull() ? null : obj.get("course_code").getAsString(),
            categoryName,
            obj.get("brand").isJsonNull() ? null : obj.get("brand").getAsString(),
            obj.get("category_id").isJsonNull() ? null : obj.get("category_id").getAsInt(),
            obj.has("condition") && !obj.get("condition").isJsonNull() ? obj.get("condition").getAsString() : "Used",
            obj.has("location") && !obj.get("location").isJsonNull() ? obj.get("location").getAsString() : "Campus",
            obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "active",
            obj.has("target_university") && !obj.get("target_university").isJsonNull() ? obj.get("target_university").getAsString() : null
        ));
    }

    public void deactivateListing(int id) {
        client.patch("listings?id=eq." + id, "{\"is_campaign_active\": false}");
    }

    public void updateStatus(int id, String status) {
        client.patch("listings?id=eq." + id, "{\"status\": \"" + status + "\"}");
    }

    public void updateListingPrice(int id, java.math.BigDecimal newPrice) {
        client.patch("listings?id=eq." + id, "{\"original_price\": " + newPrice.toPlainString() + "}");
    }

    public void deleteListing(int id) {
        try {
            
            String response = client.get("chats?listing_id=eq." + id + "&select=id");
            JsonArray array = gson.fromJson(response, JsonArray.class);
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    int chatId = array.get(i).getAsJsonObject().get("id").getAsInt();
                    
                    try {
                        client.delete("messages?chat_id=eq." + chatId);
                    } catch (Exception ignored) {}
                    
                    try {
                        client.delete("chats?id=eq." + chatId);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        
        client.delete("listings?id=eq." + id);
    }

    public void toggleCampaign(int id, boolean isActive, String targetUniversity) {
        JsonObject body = new JsonObject();
        body.addProperty("is_campaign_active", isActive);
        if (targetUniversity != null && !targetUniversity.isBlank()) {
            body.addProperty("target_university", targetUniversity);
        } else {
            body.add("target_university", com.google.gson.JsonNull.INSTANCE);
        }
        client.patch("listings?id=eq." + id, gson.toJson(body));
    }

    public void createListing(JsonObject listing) {
        client.post("listings", gson.toJson(listing));
    }

    public List<ListingSummaryDTO> findBySellerId(String sellerId) {
        String url = "listings?seller_id=eq." + sellerId + "&select=id,title,original_price,is_campaign_active,image_url,course_code,brand,status,target_university,categories(name)";
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        return parseListingSummaries(array);
    }

    public List<ListingSummaryDTO> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        
        StringBuilder idsStr = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            idsStr.append(ids.get(i));
            if (i < ids.size() - 1) idsStr.append(",");
        }

        String url = "listings?id=in.(" + idsStr.toString() + ")&select=id,title,original_price,is_campaign_active,image_url,course_code,brand,status,target_university,categories(name)";
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        return parseListingSummaries(array);
    }

    private List<ListingSummaryDTO> parseListingSummaries(JsonArray array) {
        List<ListingSummaryDTO> listings = new ArrayList<>();
        if (array == null) return listings;

        for (int i = 0; i < array.size(); i++) {
            JsonObject obj = array.get(i).getAsJsonObject();
            String categoryName = "";
            if (obj.has("categories") && !obj.get("categories").isJsonNull()) {
                categoryName = obj.get("categories").getAsJsonObject().get("name").getAsString();
            }
            listings.add(new ListingSummaryDTO(
                obj.get("id").getAsInt(),
                obj.get("title").getAsString(),
                obj.get("original_price").getAsBigDecimal(),
                obj.get("is_campaign_active").getAsBoolean(),
                obj.get("image_url").isJsonNull() ? null : obj.get("image_url").getAsString(),
                obj.get("course_code").isJsonNull() ? null : obj.get("course_code").getAsString(),
                categoryName,
                obj.get("brand").isJsonNull() ? null : obj.get("brand").getAsString(),
                obj.has("status") && !obj.get("status").isJsonNull() ? obj.get("status").getAsString() : "active",
                obj.has("target_university") && !obj.get("target_university").isJsonNull() ? obj.get("target_university").getAsString() : null
            ));
        }
        return listings;
    }
}
