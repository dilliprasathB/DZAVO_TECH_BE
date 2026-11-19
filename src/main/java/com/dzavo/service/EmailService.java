package com.dzavo.service;

import com.dzavo.dto.ContactRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${dzavo.mail.recipient:dilliprasath1225@gmail.com}")
    private String recipientEmail;

    @Value("${dzavo.mail.from:DZAVO Technology <noreply@dzavo.com>}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    private static final Map<String, String> SERVICE_LABELS = Map.of(
            "website-design-development", "Website Design & Development",
            "software-development", "Custom Software Architecture & Development",
            "design-systems", "Design Systems & UI/UX Precision Systems",
            "enterprise-engineering", "Enterprise Web Applications & Scalability",
            "brand-identity", "Brand & Visual Identity Systems",
            "cloud-infrastructure", "Cloud Infrastructure & DevOps",
            "other", "General Digital Engineering Consultation"
    );

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send contact form submission email
     *
     * @param request Contact form request
     * @param enquiryId Unique enquiry reference ID
     * @throws MessagingException if mime construction fails
     * @throws MailException if SMTP transmission fails
     */
    public void sendEnquiryEmail(ContactRequest request, String enquiryId) throws MessagingException, MailException {
        // Enforce recipient security: Never allow visitor to override recipient email
        String targetRecipient = (recipientEmail != null && !recipientEmail.isBlank())
                ? recipientEmail
                : "dilliprasath1225@gmail.com";

        String serviceName = SERVICE_LABELS.getOrDefault(request.getService(), request.getService());
        String submissionTime = ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' hh:mm:ss a z")
        );
        String companyDisplay = (request.getCompany() != null && !request.getCompany().isBlank())
                ? request.getCompany()
                : "Not Specified";
        String contactMethodDisplay = (request.getContactMethod() != null && !request.getContactMethod().isBlank())
                ? request.getContactMethod().toUpperCase()
                : "EMAIL";

        // Clean and validate SMTP configuration
        String cleanUsername = (smtpUsername != null) ? smtpUsername.trim() : "";
        String cleanPassword = (smtpPassword != null) ? smtpPassword.trim().replace(" ", "") : "";
        boolean isSmtpConfigured = !cleanUsername.isBlank() && !cleanPassword.isBlank();

        if (!isSmtpConfigured) {
            logger.error("================================================================================");
            logger.error("[DZAVO EMAIL ERROR] SMTP credentials are not configured in DZAVO_BE/.env!");
            logger.error("Target Recipient: {}", targetRecipient);
            logger.error("To fix this HTTP 500 error, open D:\\DZAVO\\DZAVO_BE\\.env and set:");
            logger.error("  SMTP_USERNAME={}", targetRecipient);
            logger.error("  SMTP_PASSWORD=<your-16-character-gmail-app-password>");
            logger.error("Generate an App Password at: https://myaccount.google.com/apppasswords");
            logger.error("================================================================================");
            throw new org.springframework.mail.MailSendException(
                    "SMTP credentials not configured. Please set SMTP_USERNAME and SMTP_PASSWORD (16-char Gmail App Password) in DZAVO_BE/.env."
            );
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // Determine from address
        String fromAddress = !cleanUsername.isBlank() ? cleanUsername : "dilliprasath1225@gmail.com";
        try {
            helper.setFrom(fromAddress, "DZAVO Technology");
        } catch (Exception e) {
            helper.setFrom(fromAddress);
        }
        helper.setTo(targetRecipient);
        helper.setSubject("New Project Enquiry – DZAVO Technology");

        // Set Reply-To to visitor's email so recipient can click 'Reply' directly
        try {
            helper.setReplyTo(request.getEmail(), request.getFullName());
        } catch (Exception e) {
            helper.setReplyTo(request.getEmail());
        }

        // Build Plain Text & HTML body
        String plainText = buildPlainText(request, serviceName, companyDisplay, contactMethodDisplay, submissionTime, enquiryId);
        String htmlText = buildHtmlText(request, serviceName, companyDisplay, contactMethodDisplay, submissionTime, enquiryId);

        helper.setText(plainText, htmlText);

        logger.info("[EmailService] Dispatching enquiry email {} to {} via SMTP host...", enquiryId, targetRecipient);
        mailSender.send(mimeMessage);
        logger.info("[EmailService] Enquiry email {} successfully delivered to {}", enquiryId, targetRecipient);
    }

    private String buildPlainText(ContactRequest req, String serviceName, String company,
                                  String contactMethod, String timestamp, String id) {
        return "=====================================================\n"
                + "NEW PROJECT ENQUIRY – DZAVO TECHNOLOGY\n"
                + "=====================================================\n\n"
                + "New Contact Form Submission\n\n"
                + "Full Name:                 " + req.getFullName() + "\n"
                + "Business Email:            " + req.getEmail() + "\n"
                + "Phone Number:              " + req.getPhone() + "\n"
                + "Company:                   " + company + "\n"
                + "Service Required:          " + serviceName + "\n"
                + "Project Overview:          " + req.getDescription() + "\n"
                + "Preferred Contact Channel: " + contactMethod + "\n"
                + "Submission Date & Time:    " + timestamp + "\n\n"
                + "Reference ID: " + id + "\n"
                + "Recipient: " + recipientEmail + "\n"
                + "=====================================================";
    }

    private String buildHtmlText(ContactRequest req, String serviceName, String company,
                                 String contactMethod, String timestamp, String id) {
        String cleanPhone = req.getPhone() != null ? req.getPhone().replaceAll("[^0-9]", "") : "";
        String waButtonHtml = "";
        if (!cleanPhone.isEmpty()) {
            waButtonHtml = "<td><a href=\"https://wa.me/" + cleanPhone + "\" style=\"display: inline-block; background-color: #25D366; color: #FFFFFF; font-size: 13px; font-weight: 700; text-decoration: none; padding: 12px 22px; border-radius: 6px;\">Chat on WhatsApp</a></td>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("<title>New Project Enquiry – DZAVO Technology</title>\n</head>\n")
          .append("<body style=\"margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #101B35;\">\n")
          .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #F8FAFC; padding: 32px 16px;\"><tr><td align=\"center\">\n")
          .append("<table role=\"presentation\" width=\"100%\" style=\"max-width: 620px; background-color: #FFFFFF; border-radius: 12px; overflow: hidden; border: 1px solid #E2E8F0; box-shadow: 0 4px 12px rgba(16, 27, 53, 0.05);\" cellspacing=\"0\" cellpadding=\"0\">\n")
          // Header
          .append("<tr><td style=\"background-color: #101B35; padding: 32px 32px 28px 32px; border-bottom: 4px solid #315BE8;\">\n")
          .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr><td>\n")
          .append("<span style=\"display: inline-block; font-size: 11px; font-weight: 700; letter-spacing: 0.1em; color: #94A3B8; text-transform: uppercase; margin-bottom: 6px;\">DZAVO TECHNOLOGY &bull; CLIENT PORTAL</span>\n")
          .append("<h1 style=\"margin: 0; font-size: 22px; font-weight: 800; color: #FFFFFF; letter-spacing: -0.02em;\">New Project Enquiry</h1>\n")
          .append("</td><td align=\"right\" valign=\"top\">\n")
          .append("<span style=\"background-color: rgba(49, 91, 232, 0.2); border: 1px solid #315BE8; color: #93C5FD; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; text-transform: uppercase;\">New Submission</span>\n")
          .append("</td></tr></table></td></tr>\n")
          // Intro
          .append("<tr><td style=\"padding: 24px 32px 16px 32px;\">\n")
          .append("<p style=\"margin: 0; font-size: 15px; line-height: 1.6; color: #475569;\">A visitor submitted a new project consultation request via the DZAVO Technology contact form.</p>\n")
          .append("</td></tr>\n")
          // Details Table
          .append("<tr><td style=\"padding: 0 32px 24px 32px;\">\n")
          .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden;\">\n")
          .append("<tr style=\"background-color: #F8FAFC;\"><td colspan=\"2\" style=\"padding: 12px 16px; border-bottom: 1px solid #E2E8F0;\">\n")
          .append("<strong style=\"font-size: 12px; text-transform: uppercase; letter-spacing: 0.06em; color: #64748B;\">Client Information</strong></td></tr>\n")
          .append("<tr><td width=\"38%\" style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Full Name</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 14px; font-weight: 700; color: #101B35;\">")
          .append(escapeHtml(req.getFullName())).append("</td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Business Email</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 14px; color: #315BE8; font-weight: 600;\">")
          .append("<a href=\"mailto:").append(escapeHtml(req.getEmail())).append("\" style=\"color: #315BE8; text-decoration: none;\">").append(escapeHtml(req.getEmail())).append("</a></td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Phone Number</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 14px; color: #101B35; font-weight: 600;\">")
          .append("<a href=\"tel:").append(escapeHtml(req.getPhone())).append("\" style=\"color: #101B35; text-decoration: none;\">").append(escapeHtml(req.getPhone())).append("</a></td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Company</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 14px; color: #101B35;\">")
          .append(escapeHtml(company)).append("</td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Service Required</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 14px; color: #315BE8; font-weight: 700;\">")
          .append(escapeHtml(serviceName)).append("</td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; font-weight: 600; color: #64748B;\">Preferred Contact</td><td style=\"padding: 12px 16px; border-bottom: 1px solid #F1F5F9; font-size: 13px; color: #101B35; font-weight: 600;\">")
          .append(escapeHtml(contactMethod)).append("</td></tr>\n")
          .append("<tr><td style=\"padding: 12px 16px; font-size: 13px; font-weight: 600; color: #64748B;\">Submission Time</td><td style=\"padding: 12px 16px; font-size: 13px; color: #475569;\">")
          .append(escapeHtml(timestamp)).append("</td></tr>\n")
          .append("</table></td></tr>\n")
          // Project Overview
          .append("<tr><td style=\"padding: 0 32px 28px 32px;\">\n")
          .append("<div style=\"background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 20px;\">\n")
          .append("<span style=\"display: block; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: #64748B; margin-bottom: 8px;\">Project Overview &amp; Objectives</span>\n")
          .append("<p style=\"margin: 0; font-size: 14px; line-height: 1.7; color: #101B35; white-space: pre-wrap;\">")
          .append(escapeHtml(req.getDescription())).append("</p></div></td></tr>\n")
          // Action Buttons
          .append("<tr><td style=\"padding: 0 32px 32px 32px;\" align=\"center\">\n")
          .append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\"><tr>\n")
          .append("<td style=\"padding-right: 12px;\"><a href=\"mailto:").append(escapeHtml(req.getEmail())).append("?subject=Re:%20DZAVO%20Technology%20Consultation\" style=\"display: inline-block; background-color: #315BE8; color: #FFFFFF; font-size: 13px; font-weight: 700; text-decoration: none; padding: 12px 22px; border-radius: 6px;\">Reply via Email</a></td>\n")
          .append(waButtonHtml)
          .append("</tr></table></td></tr>\n")
          // Footer
          .append("<tr><td style=\"background-color: #F1F5F9; padding: 18px 32px; border-top: 1px solid #E2E8F0; text-align: center;\">\n")
          .append("<p style=\"margin: 0; font-size: 12px; color: #64748B;\">Reference ID: <strong>").append(escapeHtml(id)).append("</strong> &bull; Dispatched to <strong>").append(escapeHtml(recipientEmail)).append("</strong></p>\n")
          .append("</td></tr></table></td></tr></table></body></html>");

        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
