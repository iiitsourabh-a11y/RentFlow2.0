package com.rentflow.backend.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query("""
        SELECT i FROM Inquiry i
        JOIN FETCH i.property p
        JOIN FETCH i.tenant t
        WHERE t.id = :tenantId
        ORDER BY i.createdAt DESC
        """)
    List<Inquiry> findByTenantIdWithDetails(@Param("tenantId") Long tenantId);

    @Query("""
        SELECT i FROM Inquiry i
        JOIN FETCH i.property p
        JOIN FETCH i.tenant t
        WHERE p.owner.id = :ownerId
        ORDER BY i.createdAt DESC
        """)
    List<Inquiry> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    @Query("""
        SELECT i FROM Inquiry i
        JOIN FETCH i.property p
        JOIN FETCH p.owner o
        JOIN FETCH i.tenant t
        WHERE i.id = :id
        """)
    Optional<Inquiry> findByIdWithDetails(@Param("id") Long id);
}
