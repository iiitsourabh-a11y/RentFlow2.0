
package com.rentflow.backend.property;

import com.rentflow.backend.user.Role;
import com.rentflow.backend.user.User;
import com.rentflow.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class PropertyService {

    private final PropertyRepository properties;
    private final UserRepository users;

    public PropertyService(PropertyRepository properties, UserRepository users) {
        this.properties = properties;
        this.users = users;
    }

    private User currentUser(String email) {
        return users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User account not found"));
    }

    private User requireOwner(String email) {
        User user = currentUser(email);

        if (user.getRole() != Role.OWNER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only property owners can perform this action");
        }

        return user;
    }

    private Property findProperty(Long id) {
        return properties.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Property not found"));
    }

    private void requireOwnership(Property property, User user) {
        if (!property.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You can modify only your own properties");
        }
    }

    @Transactional
    public PropertyResponse create(String email, PropertyRequest request) {
        User owner = requireOwner(email);

        Property property = new Property();
        apply(property, request, true);
        property.setOwner(owner);

        return PropertyResponse.from(properties.save(property));
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> list(
            String city,
            BigDecimal minRent,
            BigDecimal maxRent,
            int page,
            int size,
            String sortBy,
            String direction) {

        if ((minRent != null && minRent.signum() < 0)
                || (maxRent != null && maxRent.signum() < 0)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Rent limits cannot be negative");
        }

        if (minRent != null && maxRent != null
                && minRent.compareTo(maxRent) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "minRent cannot exceed maxRent");
        }

        if (page < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "size must be between 1 and 100");
        }

        String normalizedCity =
            (city == null || city.isBlank()) ? null : city.trim();

        String normalizedSortBy =
            (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;

        if (!java.util.Set.of(
                "createdAt", "updatedAt", "rentAmount", "title", "city")
                .contains(normalizedSortBy)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "sortBy must be one of: createdAt, updatedAt, rentAmount, title, city");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(
                direction == null || direction.isBlank() ? "desc" : direction);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "direction must be 'asc' or 'desc'");
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(sortDirection, normalizedSortBy));

        return properties.searchAvailable(
                normalizedCity, minRent, maxRent, pageable)
            .map(PropertyResponse::from);
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        return PropertyResponse.from(findProperty(id));
    }

    @Transactional
    public PropertyResponse update(
            String email, Long id, PropertyRequest request) {

        User owner = requireOwner(email);
        Property property = findProperty(id);

        requireOwnership(property, owner);
        apply(property, request, false);

        return PropertyResponse.from(properties.save(property));
    }

    @Transactional
    public void delete(String email, Long id) {
        User owner = requireOwner(email);
        Property property = findProperty(id);

        requireOwnership(property, owner);
        properties.delete(property);
    }

    private void apply(
            Property property, PropertyRequest request, boolean creating) {

        property.setTitle(request.title().trim());
        property.setDescription(request.description());
        property.setRentAmount(request.rentAmount());
        property.setPropertyType(request.propertyType().trim());
        property.setAddress(request.address().trim());
        property.setCity(request.city().trim());
        property.setBedrooms(request.bedrooms());
        property.setBathrooms(request.bathrooms());

        if (request.available() != null) {
            property.setAvailable(request.available());
        } else if (creating) {
            property.setAvailable(true);
        }
    }
}
