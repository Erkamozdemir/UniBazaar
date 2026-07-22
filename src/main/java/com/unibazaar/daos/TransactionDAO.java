package com.unibazaar.daos;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.unibazaar.db.SupabaseClient;
import com.unibazaar.dtos.TransactionDTO;
import com.unibazaar.dtos.TransactionRequestDTO;
import com.unibazaar.repositories.TransactionRepository;
import java.util.Optional;
import com.unibazaar.repositories.TransactionRepository;

public class TransactionDAO implements TransactionRepository {

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final Gson gson = new Gson();

    public void save(TransactionRequestDTO transaction) {
        JsonObject body = new JsonObject();
        body.addProperty("listing_id", transaction.listingId());
        body.addProperty("buyer_id", transaction.buyerId());
        body.addProperty("final_price", transaction.finalPrice());
        body.addProperty("transaction_date", transaction.transactionDate().toString());
        client.post("transactions", gson.toJson(body));
    }

    public boolean hasPurchased(int listingId, String buyerId) {
        String url = "transactions?listing_id=eq." + listingId + "&buyer_id=eq." + buyerId;
        String response = client.get(url);
        com.google.gson.JsonArray array = gson.fromJson(response, com.google.gson.JsonArray.class);
        return array != null && array.size() > 0;
    }

    public void updateStatus(int id, String status) {
        client.patch("transactions?id=eq." + id, "{\"status\": \"" + status + "\"}");
    }

    public Optional<TransactionDTO> findPendingByListingAndBuyer(int listingId, String buyerId) {
        String url = "transactions?listing_id=eq." + listingId + "&buyer_id=eq." + buyerId + "&status=in.(pending,buyer_confirmed,seller_confirmed)&select=id,listing_id,buyer_id,final_price,status,transaction_date";
        String response = client.get(url);
        JsonArray array = gson.fromJson(response, JsonArray.class);
        if (array == null || array.isEmpty()) {
            return Optional.empty();
        }
        JsonObject obj = array.get(0).getAsJsonObject();
        return Optional.of(new TransactionDTO(
            obj.get("id").getAsInt(),
            obj.get("listing_id").getAsInt(),
            obj.get("buyer_id").getAsString(),
            obj.get("final_price").getAsBigDecimal(),
            obj.get("status").getAsString(),
            obj.get("transaction_date").getAsString()
        ));
    }
}
