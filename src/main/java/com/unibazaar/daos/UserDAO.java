package com.unibazaar.daos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

public class UserDAO implements UserRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final Gson gson = new Gson();

    public Optional<UserProfileDTO> findById(String id) {
        String response = client.get("users?id=eq." + id + "&select=id,name,email,role,gpa,is_gpa_verified,rating,review_count");
        return parseFirst(response);
    }

    public Optional<UserProfileDTO> findByEmail(String email) {
        String response = client.get("users?email=eq." + email + "&select=id,name,email,role,gpa,is_gpa_verified,rating,review_count");
        return parseFirst(response);
    }

    public void deleteUser(String id) {
        client.delete("users?id=eq." + id);
    }

    private Optional<UserProfileDTO> parseFirst(String json) {
        JsonArray array = gson.fromJson(json, JsonArray.class);
        if (array == null || array.isEmpty()) {
            return Optional.empty();
        }
        JsonObject obj = array.get(0).getAsJsonObject();
        return Optional.of(new UserProfileDTO(
            obj.get("id").getAsString(),
            obj.get("name").getAsString(),
            obj.get("email").getAsString(),
            obj.get("role").getAsString(),
            obj.get("gpa").isJsonNull() ? null : obj.get("gpa").getAsBigDecimal(),
            obj.get("is_gpa_verified").getAsBoolean(),
            obj.has("rating") && !obj.get("rating").isJsonNull() ? obj.get("rating").getAsBigDecimal() : BigDecimal.ZERO,
            obj.has("review_count") && !obj.get("review_count").isJsonNull() ? obj.get("review_count").getAsInt() : 0
        ));
    }

    public void addReview(String reviewerId, String sellerId, int listingId, int rating, String comment) {
        JsonObject body = new JsonObject();
        body.addProperty("reviewer_id", reviewerId);
        body.addProperty("seller_id", sellerId);
        body.addProperty("listing_id", listingId);
        body.addProperty("rating", rating);
        body.addProperty("comment", comment);
        client.post("reviews", gson.toJson(body));
        
        
        Optional<UserProfileDTO> sellerOpt = findById(sellerId);
        if (sellerOpt.isPresent()) {
            UserProfileDTO seller = sellerOpt.get();
            int newCount = seller.reviewCount() + 1;
            BigDecimal currentTotal = seller.rating().multiply(BigDecimal.valueOf(seller.reviewCount()));
            BigDecimal newRating = currentTotal.add(BigDecimal.valueOf(rating)).divide(BigDecimal.valueOf(newCount), 2, java.math.RoundingMode.HALF_UP);
            
            JsonObject updateBody = new JsonObject();
            updateBody.addProperty("rating", newRating.toPlainString());
            updateBody.addProperty("review_count", newCount);
            client.patch("users?id=eq." + sellerId, gson.toJson(updateBody));
        }
    }

    public void verifyGpa(String userId, BigDecimal gpa, String transcriptUrl) {
        JsonObject body = new JsonObject();
        body.addProperty("gpa", gpa);
        body.addProperty("is_gpa_verified", true);
        
        client.patch("users?id=eq." + userId, gson.toJson(body));
    }
}
