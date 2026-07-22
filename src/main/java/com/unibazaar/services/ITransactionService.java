package com.unibazaar.services;

public interface ITransactionService {
    void purchaseItem(String buyerId, int listingId);
    void makeOffer(String buyerId, int listingId, java.math.BigDecimal offerPrice);
    boolean hasPurchased(int listingId, String buyerId);
    void confirmTransaction(int listingId, String buyerId, boolean isBuyer);
    void cancelTransaction(int listingId, String buyerId);
}
