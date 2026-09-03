# Shop Spring Boot

A Kotlin/Spring Boot 4 e-commerce application migrated from a Node.js reference implementation. Features product catalog, shopping cart, order management, PDF invoice generation, Stripe payment integration, and MinIO object storage.

## Tech Stack

| Component | Version |
|---|---|
| Java | 25.0.2 (Zulu) |
| Kotlin | 2.4.10 |
| Gradle | 9.6.1 |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.0 |
| Spring Data JPA | 2026.0.0 |
| Spring MVC | (via `spring-boot-starter-webmvc`) |
| Thymeleaf | 3.1.x |
| Postgres | 18 |
| Testcontainers | 2.0.5 |
| MinIO | 9.0.3 |
| iText | 9.7.0 |
| Stripe Java | 33.0.0 |

## Features

- **Product catalog** with pagination (2 items/page)
- **Product detail pages** with images
- **Authenticated shopping cart** — add/remove items
- **Checkout** with Stripe Checkout.js (optional, via env vars) or direct order placement
- **Order history** with PDF invoice download (iText 9)
- **Admin panel** — add/edit/delete products with MinIO image upload
- **Authentication** — signup, login, logout
- **Password reset** via email token
- **CSRF protection** on all mutating requests
- **Mobile-responsive** navigation with hamburger menu
- **Virtual threads** enabled

## Setup

### Prerequisites

- Java 25 (Zulu) — install via SDKMAN: `sdk install java 25.0.2-zulu`
- Docker and Docker Compose — for Postgres 18 and MinIO containers

### Environment Variables (Optional)

Configure Stripe keys to enable payment processing:

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_PUBLISHABLE_KEY=pk_test_...
```

Configure MinIO (defaults work with Docker Compose):

```bash
export MINIO_ENDPOINT=localhost
export MINIO_PORT=9000
export MINIO_SECURE=false
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=shop-spring-boot
```

Configure SMTP for password reset emails:

```bash
export MAIL_HOST=smtp.example.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@example.com
export MAIL_PASSWORD=your-password
```

### Running

Start dependencies (Postgres + MinIO):

```bash
docker compose up -d
```

Run the application:

```bash
JAVA_HOME=~/.sdkman/candidates/java/25.0.2-zulu ./gradlew bootRun
```

The app will be available at `http://localhost:8080`.

## Testing

Run all tests:

```bash
JAVA_HOME=~/.sdkman/candidates/java/25.0.2-zulu ./gradlew test
```

Tests include:
- Unit tests (JUnit 5 + Mockito)
- `@WebMvcTest` slices for controllers
- `@DataJpaTest` for repositories
- Full `@SpringBootTest` integration tests with Testcontainers

## Migration Notes

This is a Kotlin/Spring Boot 4 migration of the original Node.js application located at `../nodejs-complete-guide`. Key changes:

- **Spring Boot 4 split-jar artifacts**: `spring-boot-starter-webmvc`, `spring-boot-jpa-test`, etc.
- **MinIO 9.0.3** for image storage (replaces local filesystem)
- **iText 9.7.0** for PDF invoices (replaces plain text)
- **Stripe Java 33.0.0** for payment processing (optional, via env vars)
- **Testcontainers 2.0.5** for integration testing with Postgres
- **Virtual threads** enabled via `spring.threads.virtual.enabled=true`

## License

MIT License — see [LICENSE](LICENSE) file.
