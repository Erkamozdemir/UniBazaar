package com.unibazaar.repositories;

import com.unibazaar.dtos.TransactionRequestDTO;

public interface TransactionRepository {
    void save(TransactionRequestDTO transaction);
    boolean hasPurchased(int listingId, String buyerId);
    void updateStatus(int id, String status);
    java.util.Optional<com.unibazaar.dtos.TransactionDTO> findPendingByListingAndBuyer(int listingId, String buyerId);
}
