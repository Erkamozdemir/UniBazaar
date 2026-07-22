package com.unibazaar.services.impl;

import com.google.gson.JsonObject;
import com.unibazaar.daos.CategoryDAO;
import com.unibazaar.daos.ListingDAO;
import com.unibazaar.dtos.CategoryDTO;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.ListingSummaryDTO;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.repositories.CategoryRepository;
import com.unibazaar.repositories.ListingRepository;
import com.unibazaar.services.IListingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public class ListingServiceImpl implements IListingService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public ListingServiceImpl() {
        this.listingRepository = new ListingDAO();
        this.categoryRepository = new CategoryDAO();
    }

    @Override
    public List<ListingSummaryDTO> getAllListings(String query, Integer categoryId, String brand, BigDecimal minPrice,
            
            BigDecimal maxPrice) {
        return listingRepository.findAllActive(query, categoryId, brand, minPrice, maxPrice);
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<ListingDetailDTO> getListingById(int id) {
        return listingRepository.findById(id);
    }

    @Override
    public List<ListingSummaryDTO> getListingsBySeller(String sellerId) {
        return listingRepository.findBySellerId(sellerId);
    }

    @Override
    public List<ListingSummaryDTO> getListingsByIds(List<Integer> ids) {
        return listingRepository.findByIds(ids);
    }

    @Override
    public void toggleCampaignStatus(int listingId, boolean isActive, String targetUniversity) {
        listingRepository.toggleCampaign(listingId, isActive, targetUniversity);
    }

    @Override
    public void updatePrice(int listingId, BigDecimal newPrice) {
        listingRepository.updateListingPrice(listingId, newPrice);
    }

    @Override
    public void deleteListing(int listingId) {
        listingRepository.deleteListing(listingId);
    }

    @Override
            
    public void createListing(String sellerId, Integer categoryId, String title, String description,
            BigDecimal originalPrice, String imageUrl, String courseCode, String brand) {
        JsonObject obj = new JsonObject();
        obj.addProperty("seller_id", sellerId);
        if (categoryId != null)
            
            obj.addProperty("category_id", categoryId);
        obj.addProperty("title", title);
        if (description != null)
            obj.addProperty("description", description);
        obj.addProperty("original_price", originalPrice);
        if (imageUrl != null)
            obj.addProperty("image_url", imageUrl);
        if (courseCode != null)
            obj.addProperty("course_code", courseCode);
        if (brand != null)
            obj.addProperty("brand", brand);
        obj.addProperty("is_campaign_active", true);

        listingRepository.createListing(obj);
    }

    @Override
    public BigDecimal calculateFinalPrice(UserProfileDTO buyer, ListingDetailDTO listing) {
        BigDecimal originalPrice = listing.originalPrice();

        if (!listing.isCampaignActive() || !buyer.isGpaVerified()) {
            return originalPrice;
        }

        if (listing.targetUniversity() != null && !listing.targetUniversity().isBlank()) {
            String email = buyer.email().toLowerCase();
            int atIndex = email.indexOf("@");
            if (atIndex > 0) {
                String domain = email.substring(atIndex + 1);
                if (domain.startsWith("std.") || domain.startsWith("student.")) {
                    domain = domain.substring(domain.indexOf(".") + 1);
                }
                if (!listing.targetUniversity().toLowerCase().contains(domain)) {
                    return originalPrice;
                }
            }
        }

        BigDecimal gpa = buyer.gpa();
        if (gpa == null) {
            return originalPrice;
        }

        BigDecimal discountRate;

        if (gpa.compareTo(new BigDecimal("3.00")) > 0) {
            BigDecimal excessGpa = gpa.subtract(new BigDecimal("3.00"));
            discountRate = excessGpa.multiply(new BigDecimal("0.25"));
            // Cap discount at 25% (for GPA 4.00)
            if (discountRate.compareTo(new BigDecimal("0.25")) > 0) {
                discountRate = new BigDecimal("0.25");
            }
        } else {
            return originalPrice;
        }

        BigDecimal discount = originalPrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        return originalPrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
}
