# DZAVO Technology – Java Spring Boot Email Integration Backend

Production-ready, enterprise-grade backend service built with **Java 21** and **Spring Boot 3** for the DZAVO Technology web platform.

---

## 1. Architecture & Capabilities

* **Framework**: Java 21 LTS & Spring Boot 3.3.5
* **Email Dispatcher**: Spring Boot Starter Mail (`JavaMailSender`) with TLS/SSL SMTP
* **Validation**: Jakarta Validation API (`@NotBlank`, `@Email`, `@Size`)
* **Security & Anti-Bot**:
  * Honeypot field (`_hp`) trap that drops automated bot submissions silently
  * In-memory sliding rate limiter filter (`RateLimitFilter`) protecting endpoints
  * Strict HTML entity escaping to prevent email injection attacks
  * `Reply-To` header bound to the verified visitor's email address for 1-click direct response
* **CORS**: Configured for React frontend (`http://localhost:5173`, `http://localhost:3000`)
* **Delivery Destination**: Dispatches exclusively to `dilliprasath1225@gmail.com`

---

## 2. Directory Structure

```
DZAVO_BE/
├── pom.xml
├── .env.example
├── .env
├── src/main/resources/
│   └── application.properties
└── src/main/java/com/dzavo/
    ├── DzavoApplication.java                 # Spring Boot entry point & .env auto-loader
    ├── config/
    │   ├── CorsConfig.java                   # React CORS mapping
    │   ├── MailConfig.java                   # JavaMailSender dynamic bean configuration
    │   └── RateLimitFilter.java              # Anti-abuse IP rate limiter
    ├── controller/
    │   └── ContactController.java            # POST /api/contact & GET /api/health
    ├── dto/
    │   ├── ContactRequest.java               # Incoming enquiry payload & validation
    │   └── ApiResponse.java                  # Standardized JSON response contract
    ├── exception/
    │   └── GlobalExceptionHandler.java       # Centralized 400 & 500 error management
    └── service/
        └── EmailService.java                 # Responsive HTML & plain-text email composer
```

---

## 3. Environment Variables & SMTP Setup

Configuration can be placed in `DZAVO_BE/.env` or passed via system environment variables.

### Option A: Gmail SMTP with Google App Password (Recommended)

1. Sign in to your Google Account and ensure **2-Step Verification** is turned ON at [Google Security](https://myaccount.google.com/security).
2. Go to [Google App Passwords](https://myaccount.google.com/apppasswords).
3. Create a new App Password named **DZAVO Website**.
4. Google will generate a 16-character password (e.g., `abcd efgh ijkl mnop`).
5. In `DZAVO_BE/.env`:

```env
PORT=8080
RECIPIENT_EMAIL=dilliprasath1225@gmail.com
CORS_ORIGIN=http://localhost:5173,http://localhost:3000

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=abcdefghijklmnop
MAIL_FROM=DZAVO Technology <dilliprasath1225@gmail.com>
```

### Option B: Custom SMTP / Production Mail Host (SendGrid, Zoho, AWS SES)

```env
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=your-sendgrid-api-key
MAIL_FROM=DZAVO Technology <contact@dzavo.com>
RECIPIENT_EMAIL=dilliprasath1225@gmail.com
```

---

## 4. Running the Backend

### Development Mode (Using Maven Wrapper)
From the `DZAVO_BE/` directory:
```bash
./mvnw.cmd spring-boot:run
```
Or from the repository root:
```bash
npm run backend
```

### Running the Packaged Executable Jar
```bash
cd DZAVO_BE
./mvnw.cmd clean package -DskipTests
java -jar target/dzavo-backend-1.0.0.jar
```

---

## 5. REST API Specifications

### `POST /api/contact`
Accepts a JSON payload from the React Contact Form:

```json
{
  "fullName": "Alexandra Croft",
  "email": "alexandra@croft.com",
  "phone": "+91 8610140943",
  "company": "Croft Enterprise",
  "service": "website-design-development",
  "description": "We need custom enterprise cloud software architecture and high-throughput microservices.",
  "contactMethod": "whatsapp"
}
```

#### Success Response (`HTTP 200 OK`)
```json
{
  "success": true,
  "message": "Your enquiry has been submitted successfully. We will contact you soon.",
  "id": "ENQ-MWJ8B2-108"
}
```

#### Validation Failure (`HTTP 400 Bad Request`)
```json
{
  "success": false,
  "message": "Please check the submitted information.",
  "errors": {
    "email": "Please enter a valid email address.",
    "fullName": "Full name must be between 2 and 100 characters."
  }
}
```

#### Delivery Failure (`HTTP 500 Internal Server Error`)
```json
{
  "success": false,
  "message": "We could not submit your enquiry. Please try again later."
}
```

---

## 6. Running Automated Tests

Run the full test suite (7 tests covering health checks, email delivery verification, Jakarta validation, SMTP failure masking, and honeypot traps):

```bash
cd DZAVO_BE
./mvnw.cmd test
```
