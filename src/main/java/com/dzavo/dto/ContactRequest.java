package com.dzavo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactRequest {

    @NotBlank(message = "Please enter your full name.")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters.")
    @JsonProperty("fullName")
    private String fullName;

    @NotBlank(message = "Please enter your business email address.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 150, message = "Email address cannot exceed 150 characters.")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Please enter your direct contact phone number.")
    @Size(min = 7, max = 30, message = "Please enter a valid phone number.")
    @JsonProperty("phone")
    private String phone;

    @Size(max = 150, message = "Company name cannot exceed 150 characters.")
    @JsonProperty("company")
    private String company;

    @NotBlank(message = "Please select the service required.")
    @Size(max = 100, message = "Service selection is too long.")
    @JsonProperty("service")
    private String service;

    @NotBlank(message = "Please provide an overview of your project requirements.")
    @Size(min = 15, max = 5000, message = "Project overview must be between 15 and 5000 characters.")
    @JsonProperty("description")
    private String description;

    @Size(max = 50, message = "Contact method is too long.")
    @JsonProperty("contactMethod")
    private String contactMethod;

    /**
     * Anti-Spam Honeypot field.
     * Hidden in the frontend UI. If populated, it indicates an automated bot.
     */
    @JsonProperty("_hp")
    private String _hp;

    public ContactRequest() {
    }

    public ContactRequest(String fullName, String email, String phone, String company,
                          String service, String description, String contactMethod) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.company = company;
        this.service = service;
        this.description = description;
        this.contactMethod = contactMethod;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactMethod() {
        return contactMethod;
    }

    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    public String get_hp() {
        return _hp;
    }

    public void set_hp(String _hp) {
        this._hp = _hp;
    }
}
