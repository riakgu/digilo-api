# Digilo API

## Overview
Digilo API is a digital product e-commerce backend for selling digital goods such as game accounts, software licenses, and digital vouchers.
It features product management, shopping cart, order processing, Midtrans QRIS payment integration, promo codes, and secure credential delivery.

### Built With
[![Java][Java]][Java-url] [![Spring Boot][Spring-Boot]][Spring-Boot-url] [![PostgreSQL][PostgreSQL]][PostgreSQL-url] [![Redis][Redis]][Redis-url] [![Midtrans][Midtrans]][Midtrans-url]

[![CI](https://github.com/riakgu/digilo-api/actions/workflows/ci.yml/badge.svg)](https://github.com/riakgu/digilo-api/actions/workflows/ci.yml)


## Features

- **Product Catalog** - Categories, products, variants with flexible pricing
- **Inventory Management** - Secure encrypted credential storage (AUTO/MANUAL/HYBRID delivery)
- **Shopping Cart** - Persistent cart with stock validation
- **Order Processing** - Order creation, status tracking, inventory reservation
- **Payment Integration** - Midtrans QRIS payment with webhook notifications
- **Promo System** - Discount codes with usage limits and validation
- **Credential Delivery** - Secure decryption and delivery for paid orders
- **Image Storage** - Cloudflare R2 integration for product images


## Getting Started

### Prerequisites
* Java 25
* PostgreSQL >= 18.0
* Redis >= 8.0
* Maven >= 3.9

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/riakgu/digilo-api.git
   cd digilo-api
   ```

2. Set up local configuration
   ```sh
   cp src/main/resources/application-local.yaml.template src/main/resources/application-local.yaml
   ```

   Edit `application-local.yaml` with your configuration.

3. Start dependencies (PostgreSQL, Redis)
   ```sh
   # Using Docker (optional)
   docker run -d --name digilo-db -e POSTGRES_DB=digilo -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:18-alpine
   docker run -d --name digilo-redis -p 6379:6379 redis:8-alpine
   ```

4. Run the application
   ```sh
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8080`


### Docker

1. Set up environment variables
   ```sh
   cp .env.example .env
   ```

   Edit `.env` with your configuration.

2. Run with Docker Compose
   ```sh
   docker compose up -d
   ```

3. Build and run after code changes
   ```sh
   docker compose up -d --build
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
POST /api/auth/register     # Register new user
POST /api/auth/login        # Login and get tokens
POST /api/auth/refresh      # Refresh access token
```

### Products
```
GET  /api/public/products           # List all products
GET  /api/public/products/{id}      # Get product details
GET  /api/public/categories         # List categories
POST /api/admin/products            # Create product (admin)
```

### Cart
```
GET    /api/user/cart               # Get user's cart
POST   /api/user/cart/items         # Add item to cart
PUT    /api/user/cart/items/{id}    # Update cart item
DELETE /api/user/cart/items/{id}    # Remove cart item
```

### Orders
```
POST /api/user/orders               # Create order from cart
GET  /api/user/orders               # List user's orders
GET  /api/user/orders/{id}          # Get order details
GET  /api/user/orders/{id}/credentials  # Get purchased credentials
```

### Payments
```
POST /api/user/payments             # Create payment (QRIS)
GET  /api/user/payments/{id}        # Get payment status
POST /api/public/payments/notification  # Midtrans webhook
```

### Promos
```
POST /api/user/promos/validate      # Validate promo code
POST /api/admin/promos              # Create promo (admin)
```


## Running Tests

```sh
./mvnw test
```


## Project Structure

```
src/main/java/com/riakgu/digilo/
├── auth/           # Authentication & JWT
├── cart/           # Shopping cart
├── category/       # Product categories
├── common/         # Shared utilities, exceptions
├── config/         # App configuration
├── order/          # Order processing
├── payment/        # Midtrans integration
├── product/        # Products, variants, inventory
├── promo/          # Promo codes
└── user/           # User management
```


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
[Midtrans]: https://img.shields.io/badge/Midtrans-00529C?style=for-the-badge&logoColor=white
[Midtrans-url]: https://midtrans.com/
