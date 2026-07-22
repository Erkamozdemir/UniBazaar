package com.unibazaar.repositories;

import com.google.gson.JsonObject;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.ListingSummaryDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingRepository {
    List<ListingSummaryDTO> findAllActive(String query, Integer categoryId, String brand, BigDecimal minPrice, BigDecimal maxPrice);
    List<ListingSummaryDTO> findBySellerId(String sellerId);
    List<ListingSummaryDTO> findByIds(List<Integer> ids);
    Optional<ListingDetailDTO> findById(int id);
    void updateListingPrice(int id, BigDecimal newPrice);
    void deleteListing(int id);
    void deactivateListing(int id);
    void updateStatus(int id, String status);
    void toggleCampaign(int id, boolean isActive, String targetUniversity);
    void createListing(JsonObject listing);
}
