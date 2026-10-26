
package com.rentflow.backend.property;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService service;

    public PropertyController(PropertyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(
            @Valid @RequestBody PropertyRequest request,
            Principal principal) {
        return service.create(principal.getName(), request);
    }

    @GetMapping
    public Page<PropertyResponse> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return service.list(
            city, minRent, maxRent, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public PropertyResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request,
            Principal principal) {
        return service.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        service.delete(principal.getName(), id);
    }
}
