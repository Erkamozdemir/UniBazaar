package com.unibazaar.services.impl;

import com.unibazaar.daos.ListingDAO;
import com.unibazaar.daos.TransactionDAO;
import com.unibazaar.daos.UserDAO;
import com.unibazaar.dtos.ListingDetailDTO;
import com.unibazaar.dtos.TransactionDTO;
import com.unibazaar.dtos.TransactionRequestDTO;
import com.unibazaar.dtos.UserProfileDTO;
import com.unibazaar.repositories.ListingRepository;
import com.unibazaar.repositories.TransactionRepository;
import com.unibazaar.repositories.UserRepository;
import com.unibazaar.services.IListingService;
import com.unibazaar.services.ITransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public class TransactionServiceImpl implements ITransactionService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final IListingService listingService;

    public TransactionServiceImpl() {
        this.userRepository = new UserDAO();
        this.listingRepository = new ListingDAO();
        this.transactionRepository = new TransactionDAO();
        this.listingService = new ListingServiceImpl();
    }

    @Override
    public void purchaseItem(String buyerId, int listingId) {
        Optional<UserProfileDTO> buyerOpt = userRepository.findById(buyerId);
        if (buyerOpt.isEmpty()) {
            throw new IllegalArgumentException("Buyer not found with id: " + buyerId);
        }

        UserProfileDTO buyer = buyerOpt.get();

        if (!"STUDENT".equals(buyer.role())) {
            throw new SecurityException("Only users with STUDENT role can make purchases.");
        }

        if (buyer.email() == null || !buyer.email().endsWith(".edu.tr")) {
            throw new SecurityException("Only users with a valid .edu.tr email can make purchases.");
        }

        Optional<ListingDetailDTO> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isEmpty()) {
            throw new IllegalArgumentException("Listing not found with id: " + listingId);
        }

        ListingDetailDTO listing = listingOpt.get();
        BigDecimal finalPrice = listingService.calculateFinalPrice(buyer, listing);

        TransactionRequestDTO transaction = new TransactionRequestDTO(
            listingId,
            buyerId,
            finalPrice,
            LocalDateTime.now()
        );

        transactionRepository.save(transaction);
        listingRepository.updateStatus(listingId, "pending");
    }

    @Override
    public void makeOffer(String buyerId, int listingId, BigDecimal offerPrice) {
        Optional<UserProfileDTO> buyerOpt = userRepository.findById(buyerId);
        if (buyerOpt.isEmpty()) {
            throw new IllegalArgumentException("Buyer not found.");
        }
        Optional<ListingDetailDTO> listingOpt = listingRepository.findById(listingId);
        if (listingOpt.isEmpty()) {
            throw new IllegalArgumentException("Listing not found.");
        }

        TransactionRequestDTO transaction = new TransactionRequestDTO(
            listingId,
            buyerId,
            offerPrice,
            LocalDateTime.now()
        );
        transactionRepository.save(transaction);
        listingRepository.updateStatus(listingId, "pending");
    }

    @Override
    public void confirmTransaction(int listingId, String buyerId, boolean isBuyer) {
        Optional<TransactionDTO> transactionOpt = transactionRepository.findPendingByListingAndBuyer(listingId, buyerId);
        if (transactionOpt.isEmpty()) {
            return;
        }
        
        TransactionDTO transaction = transactionOpt.get();
        String currentStatus = transaction.status();
        String newStatus = currentStatus;
        
        if ("pending".equals(currentStatus)) {
            newStatus = isBuyer ? "buyer_confirmed" : "seller_confirmed";
        } else if (("seller_confirmed".equals(currentStatus) && isBuyer) ||
                   ("buyer_confirmed".equals(currentStatus) && !isBuyer)) {
            newStatus = "completed";
            listingRepository.updateStatus(listingId, "sold");
        }
        
        if (!newStatus.equals(currentStatus)) {
            transactionRepository.updateStatus(transaction.id(), newStatus);
        }
    }

    @Override
    public void cancelTransaction(int listingId, String buyerId) {
        Optional<TransactionDTO> transactionOpt = transactionRepository.findPendingByListingAndBuyer(listingId, buyerId);
        if (transactionOpt.isPresent()) {
            transactionRepository.updateStatus(transactionOpt.get().id(), "cancelled");
        }
        listingRepository.updateStatus(listingId, "active");
    }

    @Override
    public boolean hasPurchased(int listingId, String buyerId) {
        return transactionRepository.hasPurchased(listingId, buyerId);
    }
}
