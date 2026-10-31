
package com.rentflow.backend.property;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByAvailableTrueOrderByCreatedAtDesc();

    List<Property> findByCityIgnoreCaseAndAvailableTrueOrderByCreatedAtDesc(
            String city);

    List<Property> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query(value = """
        SELECT p FROM Property p
        JOIN FETCH p.owner
        WHERE p.available = true
          AND (COALESCE(:city, '') = '' OR LOWER(p.city) = LOWER(:city))
          AND (:minRent IS NULL OR p.rentAmount >= :minRent)
          AND (:maxRent IS NULL OR p.rentAmount <= :maxRent)
        """,
        countQuery = """
        SELECT count(p) FROM Property p
        WHERE p.available = true
          AND (COALESCE(:city, '') = '' OR LOWER(p.city) = LOWER(:city))
          AND (:minRent IS NULL OR p.rentAmount >= :minRent)
          AND (:maxRent IS NULL OR p.rentAmount <= :maxRent)
        """)
    Page<Property> searchAvailable(
            @Param("city") String city,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            Pageable pageable
    );

    @Query("SELECT p FROM Property p JOIN FETCH p.owner WHERE p.id = :id")
    java.util.Optional<Property> findByIdWithOwner(@Param("id") Long id);
}
