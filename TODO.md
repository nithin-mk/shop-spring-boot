# Shop Spring Boot — Migration TODO

## Tech Stack
| Component | Version |
|---|---|
| Java | 25.0.2 (Zulu, via sdkman) |
| Kotlin | 2.4.10 |
| Gradle | 9.6.1 |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.0 |
| Spring Data JPA | 2026.0.0 |
| Spring MVC | (via spring-boot-starter-webmvc) |
| Thymeleaf | 3.1.x |
| Postgres | 18 (Docker image: postgres:18) |
| Testcontainers | 2.0.5 |
| Virtual Threads | Enabled via `spring.threads.virtual.enabled=true` |

## Plan

- [x] Create project structure (`settings.gradle.kts`, `build.gradle.kts`)
- [x] Gradle wrapper initialised (using installed Gradle 9.6.1)
- [x] JPA Entities: `User`, `Product`, `CartItem`, `Order`, `OrderItem`, `PasswordResetToken`
- [x] Spring Data JPA Repositories
- [x] Spring Security configuration (email-based login, BCrypt, CSRF)
- [x] Service layer: `UserService`, `ProductService`, `CartService`, `OrderService`
- [x] MVC Controllers: `ShopController`, `AdminController`, `AuthController`, `ErrorController`
- [x] Thymeleaf templates (converted from EJS, full mobile responsiveness preserved)
- [x] Static assets (CSS, JS) copied from original
- [x] `application.properties` (virtual threads, JPA, mail)
- [x] `compose.yaml` (Postgres 18, Spring Boot Docker Compose auto-start)
- [x] Unit tests (JUnit 5 + Mockito): `ProductServiceTest`, `CartServiceTest`, `UserServiceTest`
- [x] Integration tests (`@WebMvcTest`, `@DataJpaTest`, TestContainers full slice)
- [x] Run tests: `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-zulu ./gradlew test` — all passing
- [x] Run app: `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-zulu ./gradlew bootRun` — running on port 8080

## Behaviour Preserved from Node.js App
- Public shop/product listing with pagination (2 items/page)
- Product detail page
- Authenticated cart: add, remove items
- Checkout page with Stripe Checkout.js (when `STRIPE_SECRET_KEY` env var set) or plain button
- Orders page + **real iText 9.7.0 PDF invoice download** (was plain text)
- Admin: add/edit/delete products with **MinIO 9.0.3 image storage** (was local filesystem)
- Auth: signup (email+password), login, logout
- Password reset via token (email link) with flash error messages
- CSRF protection on all mutating requests
- Mobile-responsive nav (hamburger menu + side drawer)
- Error pages: 404, 500

## High-Priority Gaps Fixed (Checkpoint 6→7)

### 1. MinIO Image Storage ✅
- **Dependency**: `io.minio:minio:9.0.3`
- **Service**: `ImageStorageService` now uploads to MinIO, generates 1-hour presigned GET URLs
- **Config**: `application.properties` has `shop.minio.*` properties (endpoint, port, secure, access-key, secret-key, bucket)
- **Docker**: `compose.yaml` includes MinIO service (port 9000/9001, volume-backed)
- **Templates**: All product image renders use presigned URLs from MinIO instead of local paths
- **API notes**: MinIO 9 uses `Http.Method.GET` (nested class), `PutObjectArgs.stream(stream, size, -1L)` with Long params

### 2. Real PDF Invoice Generation ✅
- **Dependency**: `com.itextpdf:kernel:9.7.0`, `com.itextpdf:io:9.7.0`, `com.itextpdf:layout:9.7.0`
- **Implementation**: `ShopController.generateInvoicePdf()` uses iText 9 to create formatted PDF (bold 26pt title, 14pt items, 20pt total)
- **API notes**: iText 9 has no `setBold()` — use `PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)` then `.setFont()`

### 3. Stripe Payment Integration ✅
- **Dependency**: `com.stripe:stripe-java:33.0.0`
- **Service**: `StripeService` wraps Stripe client, `enabled` flag when `STRIPE_SECRET_KEY` env var is set
- **Checkout flow**: 
  - If Stripe enabled: shows Stripe Checkout.js hosted button (legacy v2 API, matches original EJS)
  - If Stripe disabled: shows plain "Place Order" button
- **Order creation**: `ShopController.createOrder()` charges via Stripe before saving order (if enabled), returns to checkout with error on failure
- **API notes**: stripe-java 33 uses `v1().charges().create()` (non-deprecated path)

### 4. Flash Error Messages ✅
- Already working correctly — `AuthController` uses `?error` query param + `RedirectAttributes`
- All auth templates render `errorMessage` from model

## Notes
- **Stripe**: Optional via `STRIPE_SECRET_KEY` + `STRIPE_PUBLISHABLE_KEY` env vars. Without them, checkout shows plain button.
- **MinIO**: Configured via `MINIO_*` env vars. Docker Compose auto-starts MinIO on port 9000.
- **Email**: Spring Boot Mail (configure SMTP via `MAIL_*` env vars)
- **All tests passing**: 54 tests (unit, @WebMvcTest slices, @DataJpaTest + Testcontainers, @SpringBootTest integration)
