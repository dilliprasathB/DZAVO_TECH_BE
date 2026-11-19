package com.dzavo.controller;

import com.dzavo.dto.ApiResponse;
import com.dzavo.dto.ContactRequest;
import com.dzavo.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "DZAVO Technology Java Backend",
                "recipient", "dilliprasath1225@gmail.com",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Contact Form Submission API
     * POST /api/contact
     */
    @PostMapping("/contact")
    public ResponseEntity<ApiResponse> submitContactForm(@Valid @RequestBody ContactRequest request)
            throws MessagingException, MailException {

        // Anti-Spam: Honeypot check
        if (request.get_hp() != null && !request.get_hp().isBlank()) {
            logger.warn("[Anti-Spam] Bot detected via honeypot trap. Silently dropping email transmission.");
            return ResponseEntity.ok(ApiResponse.success(
                    "Your enquiry has been submitted successfully. We will contact you soon.",
                    "ENQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            ));
        }

        // Generate unique reference ID
        String enquiryId = "ENQ-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                + "-" + (int) (100 + Math.random() * 900);

        logger.info("[ContactController] Received valid enquiry {} from {}", enquiryId, request.getEmail());

        // Dispatch Email via JavaMailSender
        emailService.sendEnquiryEmail(request, enquiryId);

        // Success response
        return ResponseEntity.ok(ApiResponse.success(
                "Your enquiry has been submitted successfully. We will contact you soon.",
                enquiryId
        ));
    }

    /**
     * Backward-compatible alias for /api/enquiries
     */
    @PostMapping("/enquiries")
    public ResponseEntity<ApiResponse> submitEnquiriesAlias(@Valid @RequestBody ContactRequest request)
            throws MessagingException, MailException {
        return submitContactForm(request);
    }
}
