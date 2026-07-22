package com.unibazaar.repositories;

import com.unibazaar.dtos.UserProfileDTO;
import java.util.Optional;

public interface UserRepository {
    Optional<UserProfileDTO> findById(String id);
    Optional<UserProfileDTO> findByEmail(String email);
    void deleteUser(String id);
    void addReview(String reviewerId, String sellerId, int listingId, int rating, String comment);
    void verifyGpa(String userId, java.math.BigDecimal gpa, String transcriptUrl);
}
