# Inventory Management System

A Spring Boot web application for managing inventory, backed by an in-memory H2 database.

## Tech Stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Data JPA, Bean Validation
- H2 (in-memory database)
- JUnit 5, Mockito, MockMvc for testing
- Travis CI for continuous integration, Docker for deployment

## Running Locally

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. The H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:inventorydb`, user: `sa`, no password).

## Running Tests

```bash
mvn test
```

## API Endpoints

Base path: `/api/v1/inventory`

| Method | Path                          | Description                                  |
|--------|-------------------------------|----------------------------------------------|
| GET    | `/`                           | List all inventory items                     |
| GET    | `/{id}`                       | Get an item by id                            |
| GET    | `/sku/{sku}`                  | Get an item by SKU                           |
| GET    | `/category/{category}`        | List items in a category                     |
| GET    | `/low-stock?threshold=10`     | List items with quantity below the threshold |
| POST   | `/`                           | Create a new item                            |
| PUT    | `/{id}`                       | Update an existing item                      |
| PATCH  | `/{id}/adjust-quantity?delta=`| Increase/decrease stock by a delta           |
| DELETE | `/{id}`                       | Delete an item                               |

### Example: create an item

```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "sku": "SKU-001",
    "description": "Dell XPS 15",
    "quantity": 25,
    "price": 1299.99,
    "category": "Electronics"
  }'
```

## Docker

```bash
docker build -t inventory-management .
docker run -p 8080:8080 inventory-management
```

## CI

Travis CI runs `mvn clean verify` on every push (see `.travis.yml`).
