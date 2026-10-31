package com.rentflow.backend.inquiry;

import com.rentflow.backend.property.Property;
import com.rentflow.backend.property.PropertyRepository;
import com.rentflow.backend.user.Role;
import com.rentflow.backend.user.User;
import com.rentflow.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class InquiryService {

    private final InquiryRepository inquiries;
    private final PropertyRepository properties;
    private final UserRepository users;

    public InquiryService(
        InquiryRepository inquiries,
        PropertyRepository properties,
        UserRepository users
    ) {
        this.inquiries = inquiries;
        this.properties = properties;
        this.users = users;
    }

    private User currentUser(String email) {
        return users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User account not found"));
    }

    @Transactional
    public InquiryResponse create(String email, Long propertyId, InquiryRequest request) {
        User tenant = currentUser(email);

        if (tenant.getRole() != Role.TENANT) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only registered tenants can submit property inquiries");
        }

        Property property = properties.findByIdWithOwner(propertyId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Property not found"));

        if (!property.isAvailable()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot submit an inquiry for an unavailable property");
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setProperty(property);
        inquiry.setTenant(tenant);
        inquiry.setMessage(request.message().trim());
        inquiry.setStatus(InquiryStatus.PENDING);

        return InquiryResponse.from(inquiries.save(inquiry));
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listForTenant(String email) {
        User tenant = currentUser(email);
        return inquiries.findByTenantIdWithDetails(tenant.getId())
            .stream()
            .map(InquiryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> listForOwner(String email) {
        User owner = currentUser(email);
        if (owner.getRole() != Role.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only property owners can view received inquiries");
        }

        return inquiries.findByOwnerIdWithDetails(owner.getId())
            .stream()
            .map(InquiryResponse::from)
            .toList();
    }

    @Transactional
    public InquiryResponse updateStatus(String email, Long inquiryId, InquiryStatus newStatus) {
        User owner = currentUser(email);
        Inquiry inquiry = inquiries.findByIdWithDetails(inquiryId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inquiry not found"));

        if (!inquiry.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can only manage inquiries for your own properties");
        }

        inquiry.setStatus(newStatus);
        return InquiryResponse.from(inquiries.save(inquiry));
    }
}
