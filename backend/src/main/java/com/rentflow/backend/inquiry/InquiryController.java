package com.rentflow.backend.inquiry;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService service;

    public InquiryController(InquiryService service) {
        this.service = service;
    }

    @PostMapping("/property/{propertyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(
            @PathVariable Long propertyId,
            @Valid @RequestBody InquiryRequest request,
            Principal principal) {
        return service.create(principal.getName(), propertyId, request);
    }

    @GetMapping("/sent")
    public List<InquiryResponse> listSent(Principal principal) {
        return service.listForTenant(principal.getName());
    }

    @GetMapping("/received")
    public List<InquiryResponse> listReceived(Principal principal) {
        return service.listForOwner(principal.getName());
    }

    @PatchMapping("/{id}/status")
    public InquiryResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InquiryStatusUpdateRequest request,
            Principal principal) {
        return service.updateStatus(principal.getName(), id, request.status());
    }
}
