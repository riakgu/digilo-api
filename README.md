# Digilo API

## Overview
Digilo API is a digital product e-commerce backend for selling digital goods such as game accounts, software licenses, and digital vouchers.
It features product management, shopping cart, order processing, Midtrans QRIS payment integration, promo codes, secure credential delivery, and real-time notifications.

### Built With
[![Java][Java]][Java-url] [![Spring Boot][Spring-Boot]][Spring-Boot-url] [![PostgreSQL][PostgreSQL]][PostgreSQL-url] [![Redis][Redis]][Redis-url] [![Kafka][Kafka]][Kafka-url] [![Midtrans][Midtrans]][Midtrans-url]

[![CI](https://github.com/riakgu/digilo-api/actions/workflows/ci.yml/badge.svg)](https://github.com/riakgu/digilo-api/actions/workflows/ci.yml)

## Features

### Authentication & Users
- User registration and login with JWT tokens
- Google OAuth integration
- Password reset with email OTP
- Phone verification via WhatsApp OTP
- Email verification
- User profile management
- Admin user management

### Products & Catalog
- Product CRUD with slug-based URLs
- Product search and category filtering
- Multiple sort options (price, newest, best selling)
- Product variants (e.g., 1 month, 3 months subscription)
- Product images with Cloudflare R2 storage
- Image reordering and primary image selection

### Inventory Management
- Credential-based inventory (encrypted storage)
- Bulk inventory creation
- Inventory reservation system
- Stock tracking per variant

### Shopping & Orders
- Shopping cart (add, update, remove, clear)
- Order creation from cart
- Order status tracking (PENDING, PAID, CANCELLED, FAILED)
- Credential delivery upon payment

### Payments
- Midtrans payment gateway integration
- Payment webhook handling
- Payment status synchronization

### Promotions
- Promo code system
- Percentage and fixed discount types
- Usage limit tracking
- Promo validation

### Notifications
- In-app notification system
- Kafka event-driven notifications
- Order and payment status updates
- Unread count tracking

### Admin Dashboard
- Dashboard statistics
- Top users by spending
- Top selling products
- Recent orders
- Sales chart with period filtering

## Getting Started

### Prerequisites
* Docker & Docker Compose
* Maven >= 3.9 (for development only)

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/riakgu/digilo-api.git
   cd digilo-api
   ```

2. Set up environment variables
   ```sh
   cp .env.example .env
   ```
   Edit `.env` with your configuration.

3. Start all services
   ```sh
   docker compose up -d
   ```
   The API will be available at `http://localhost:8080`

4. Rebuild after code changes
   ```sh
   docker compose up -d --build
   ```

### Seed Data (Optional)

To populate the database with dummy data for frontend development:

```sh
# Clear existing data (if re-seeding)
docker exec -i digilo-db psql -U $POSTGRES_USER -d digilo < src/main/resources/db/clear-data.sql

# Seed data
docker exec -i digilo-db psql -U $POSTGRES_USER -d digilo < src/main/resources/db/seed-data.sql
```

**Regenerate encrypted credentials** (if encryption keys change):
```sh
# Update ENCRYPTION_PASSWORD & ENCRYPTION_SALT in CredentialGenerator.java
mvn exec:java -D exec.mainClass="com.riakgu.digilo.util.CredentialGenerator" -D exec.classpathScope="test"
# Copy the output to replace section 7 in seed-data.sql
```


## Environment Profiles

| Profile | Purpose | Config |
|---------|---------|--------|
| `local` | IDE/local development | Hardcoded values |
| `dev` | Docker development | Environment variables |
| `staging` | Pre-production testing | Environment variables |
| `prod` | Production | Environment variables |
| `test` | CI/Testing | Hardcoded test values |

```sh
# Set profile via environment variable
SPRING_PROFILES_ACTIVE=staging docker compose up -d
```


## API Reference

### Authentication
```
POST /api/auth/register          # Register new user
POST /api/auth/login             # Login with email/password
POST /api/auth/google            # Login with Google OAuth
POST /api/auth/refresh           # Refresh access token
POST /api/auth/logout            # Logout and invalidate tokens
POST /api/auth/password/forgot   # Request password reset OTP
POST /api/auth/password/verify   # Verify reset OTP
PUT  /api/auth/password/reset    # Reset password with token
POST /api/auth/email/send-otp    # Send email verification OTP
POST /api/auth/email/verify      # Verify email
POST /api/auth/phone/send-otp    # Send phone verification OTP (WhatsApp)
POST /api/auth/phone/verify      # Verify phone
```

### User
```
GET   /api/user/profile          # Get current user profile
PATCH /api/user/profile          # Update profile
PATCH /api/user/password         # Change password
GET   /api/admin/users           # List all users (admin)
GET   /api/admin/users/{id}      # Get user by ID (admin)
PATCH /api/admin/users/{id}      # Update user role/status (admin)
```

### Products
```
GET  /api/public/products              # List active products
GET  /api/public/products/{slug}       # Get product details
GET  /api/public/products/search       # Search products
GET  /api/public/categories            # List active categories
GET  /api/public/categories/{slug}     # Get category details
POST /api/admin/products               # Create product (admin)
PUT  /api/admin/products/{id}          # Update product (admin)
```

### Cart
```
GET    /api/user/cart                  # Get user's cart
POST   /api/user/cart/items            # Add item to cart
PUT    /api/user/cart/items/{id}       # Update cart item
DELETE /api/user/cart/items/{id}       # Remove cart item
DELETE /api/user/cart                  # Clear cart
```

### Orders
```
POST /api/user/orders                  # Create order from cart
GET  /api/user/orders                  # List user's orders
GET  /api/user/orders/{id}             # Get order details
GET  /api/user/orders/{id}/credentials # Get purchased credentials
GET  /api/admin/orders                 # List all orders (admin)
PATCH /api/admin/orders/{id}/status    # Update order status (admin)
```

### Payments
```
POST /api/user/payments                # Create payment (QRIS)
GET  /api/user/payments                # List user's payments
GET  /api/user/payments/{id}           # Get payment status
POST /api/user/payments/{id}/sync      # Sync payment status
POST /api/public/payments/notification # Midtrans webhook
```

### Promos
```
POST /api/user/promos/validate         # Validate promo code
GET  /api/admin/promos                 # List promos (admin)
POST /api/admin/promos                 # Create promo (admin)
PUT  /api/admin/promos/{id}            # Update promo (admin)
```

### Dashboard (Admin)
```
GET /api/admin/dashboard/stats         # Overview statistics
GET /api/admin/dashboard/top-users     # Top spending users
GET /api/admin/dashboard/top-products  # Top selling products
GET /api/admin/dashboard/recent-orders # Recent orders
GET /api/admin/dashboard/sales-chart   # Sales chart data
```

### Notifications
```
GET   /api/user/notifications              # Get notifications
GET   /api/user/notifications/unread-count # Unread count
PATCH /api/user/notifications/{id}/read    # Mark as read
PATCH /api/user/notifications/read-all     # Mark all as read
```


## Running Tests

Tests use **Testcontainers** to automatically spin up PostgreSQL, Redis, and Kafka containers.

### Run Tests

```sh
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthControllerTest

# Run with verbose output
./mvnw test -X
```

### Test Configuration
Tests use `application-test.yaml` which:
- Uses **Testcontainers** for PostgreSQL, Redis, and Kafka (auto-managed)
- Mocks external services (Email, Midtrans, WhatsApp, R2 Storage)
- No manual database setup required

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


[Java]: https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://openjdk.org/
[Spring-Boot]: https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white
[Spring-Boot-url]: https://spring.io/projects/spring-boot
[PostgreSQL]: https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white
[PostgreSQL-url]: https://www.postgresql.org/
[Redis]: https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white
[Redis-url]: https://redis.io/
[Kafka]: https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white
[Kafka-url]: https://kafka.apache.org/
[Midtrans]: https://img.shields.io/badge/Midtrans-00529C?style=for-the-badge&logoColor=white
[Midtrans-url]: https://midtrans.com/

