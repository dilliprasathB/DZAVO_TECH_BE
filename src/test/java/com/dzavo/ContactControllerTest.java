package com.dzavo;

import com.dzavo.dto.ContactRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.mail.username=dilliprasath1225@gmail.com",
        "spring.mail.password=mockapppassword12"
})
@AutoConfigureMockMvc
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        // Return a mock MimeMessage on createMimeMessage
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("1. Health Check returns HTTP 200 and UP status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.recipient", is("dilliprasath1225@gmail.com")));
    }

    @Test
    @DisplayName("2. Valid form submission sends email to dilliprasath1225@gmail.com and returns HTTP 200")
    void testValidSubmission_Success() throws Exception {
        ContactRequest validRequest = new ContactRequest(
                "Marcus Vance",
                "marcus.v@globaltech.io",
                "+91 8610140943",
                "GlobalTech Ventures",
                "software-development",
                "We need custom enterprise cloud software architecture and high-throughput microservices.",
                "whatsapp"
        );

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Your enquiry has been submitted successfully. We will contact you soon.")))
                .andExpect(jsonPath("$.id", notNullValue()));

        // Verify that JavaMailSender.send was called exactly once
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        // Recipient must be dilliprasath1225@gmail.com
        assertEquals("dilliprasath1225@gmail.com", sentMessage.getAllRecipients()[0].toString());
        // Subject check
        assertEquals("New Project Enquiry – DZAVO Technology", sentMessage.getSubject());
        // Reply-To check (must point to visitor's email)
        assertTrue(sentMessage.getReplyTo()[0].toString().contains("marcus.v@globaltech.io"));
    }

    @Test
    @DisplayName("3. Invalid email format returns HTTP 400 and validation error")
    void testInvalidEmail_Returns400() throws Exception {
        ContactRequest invalidEmailRequest = new ContactRequest(
                "Sarah Jenkins",
                "invalid-email-address",
                "+91 8610140943",
                "Apex Medical",
                "software-development",
                "Looking for an engineering partner to architect diagnostic software.",
                "email"
        );

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Please check the submitted information.")))
                .andExpect(jsonPath("$.errors.email", notNullValue()));

        // Email sender must NOT be called
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("4. Missing required fields returns HTTP 400")
    void testMissingRequiredFields_Returns400() throws Exception {
        ContactRequest incompleteRequest = new ContactRequest();
        incompleteRequest.setFullName("A"); // Too short
        // email, phone, service, description are all null/blank

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Please check the submitted information.")))
                .andExpect(jsonPath("$.errors.fullName", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.phone", notNullValue()))
                .andExpect(jsonPath("$.errors.service", notNullValue()))
                .andExpect(jsonPath("$.errors.description", notNullValue()));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("5. SMTP connection failure returns HTTP 500 without leaking credentials")
    void testSmtpFailure_Returns500() throws Exception {
        // Force mailSender to throw MailSendException (simulating network failure or invalid password)
        doThrow(new MailSendException("Authentication failed: invalid SMTP credentials for smtp.gmail.com"))
                .when(mailSender).send(any(MimeMessage.class));

        ContactRequest validRequest = new ContactRequest(
                "Marcus Vance",
                "marcus.v@globaltech.io",
                "+91 8610140943",
                "GlobalTech Ventures",
                "software-development",
                "We need custom enterprise cloud software architecture and high-throughput microservices.",
                "whatsapp"
        );

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("We could not submit your enquiry. Please try again later.")))
                // Verify no internal passwords or raw stack trace leaked
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("6. Honeypot anti-spam trap silently drops bot submissions without sending email")
    void testHoneypotBotTrap_SilentDrop() throws Exception {
        ContactRequest botRequest = new ContactRequest(
                "Spam Bot 3000",
                "spambot@automated.ru",
                "+91 8610140943",
                "Spam Co",
                "software-development",
                "This is automated spam that should be trapped by honeypot.",
                "email"
        );
        botRequest.set_hp("automated-fill-data");

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(botRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Your enquiry has been submitted successfully. We will contact you soon.")));

        // Verify mailSender was NOT called
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("7. Malformed JSON payload returns HTTP 400 Bad Request")
    void testMalformedJson_Returns400() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": json, syntax}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid request payload format.")));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
