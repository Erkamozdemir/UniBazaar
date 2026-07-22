package com.unibazaar.services;

import com.unibazaar.dtos.CategoryDTO;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.ListingSummaryDTO;
import com.unibazaar.dtos.UserProfileDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IListingService {
    List<ListingSummaryDTO> getAllListings(String query, Integer categoryId, String brand, BigDecimal minPrice, BigDecimal maxPrice);
    List<ListingSummaryDTO> getListingsBySeller(String sellerId);
    List<ListingSummaryDTO> getListingsByIds(List<Integer> ids);
    List<CategoryDTO> getAllCategories();
    Optional<ListingDetailDTO> getListingById(int id);
    void createListing(String sellerId, Integer categoryId, String title, String description, BigDecimal originalPrice, String imageUrl, String courseCode, String brand);
    void toggleCampaignStatus(int listingId, boolean isActive, String targetUniversity);
    void updatePrice(int listingId, BigDecimal newPrice);
    void deleteListing(int listingId);
    BigDecimal calculateFinalPrice(UserProfileDTO buyer, ListingDetailDTO listing);
}
