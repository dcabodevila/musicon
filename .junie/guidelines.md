# Musicon Project Development Guidelines

This document provides essential information for developers working on the Musicon project.

## Build and Configuration Instructions

### Prerequisites

- Java 17 (required by the project)
- PostgreSQL database
- Maven (or use the included Maven wrapper)

### Database Setup

1. Create a PostgreSQL database named `gestmusica_db`
2. Default credentials (for development):
   - Username: `postgres`
   - Password: `admin`
   - URL: `jdbc:postgresql://localhost:5432/gestmusica_db`

### Environment Variables

The application uses several environment variables that need to be set:

- `DATASOURCE_URL`: Database URL (defaults to `jdbc:postgresql://localhost:5432/gestmusica_db`)
- `DATASOURCE_USR`: Database username (defaults to `postgres`)
- `DATASOURCE_PWD`: Database password (defaults to `admin`)
- `PORT`: Server port (defaults to `8081`)
- `MAIL_PASSWORD`: Password for the email service
- `MAIL_SENDER_NAME`: Name for the email sender (defaults to `Gestmusica`)
- `MAILGUN_APIKEY`: API key for Mailgun service
- `ORQUESTASDEGALICIA_APIKEY`: API key for Orquestas de Galicia service

### Build Commands

```bash
# Clean and build the project
./mvnw clean install

# Run the application in development mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build for production
./mvnw clean package -P prod
```

### Profiles

The application supports multiple profiles:

- `dev`: Development profile with enhanced logging and development-specific settings
- `prod`: Production profile with optimized settings

## Testing Information

### Testing Framework

The project uses JUnit 5 for testing with Spring Boot Test support for integration tests.

### Running Tests

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TestClassName

# Run with a specific profile
./mvnw test -Dspring.profiles.active=dev
```

### Adding New Tests

1. Create test classes in the `src/test/java` directory following the same package structure as the main code
2. Use the `@Test` annotation from JUnit 5 for test methods
3. For Spring-related tests, use `@SpringBootTest` annotation
4. Follow the Arrange-Act-Assert pattern for test clarity

### Test Example

Here's a simple test for the `StringUtils` class:

```java
package es.musicalia.gestmusica.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void removeHttp_shouldRemoveHttpPrefix() {
        // Arrange
        String input = "http://example.com";
        
        // Act
        String result = StringUtils.removeHttp(input);
        
        // Assert
        assertEquals("example.com", result);
    }
    
    @Test
    void removeHttp_shouldRemoveHttpsPrefix() {
        // Arrange
        String input = "https://example.com";
        
        // Act
        String result = StringUtils.removeHttp(input);
        
        // Assert
        assertEquals("example.com", result);
    }
    
    @Test
    void removeHttp_shouldNotChangeStringWithoutPrefix() {
        // Arrange
        String input = "example.com";
        
        // Act
        String result = StringUtils.removeHttp(input);
        
        // Assert
        assertEquals("example.com", result);
    }
}
```

## Additional Development Information

### Project Structure

- `src/main/java`: Java source code
- `src/main/resources`: Configuration files and static resources
- `src/test/java`: Test source code

### Key Technologies

- Spring Boot 3.2.5
- Spring Data JPA
- Spring Security
- Thymeleaf
- PostgreSQL
- JasperReports
- Lombok
- MapStruct

### Caching

The application uses EhCache for caching, configured in `ehcache.xml`.

### API Documentation

Swagger/OpenAPI documentation is available at:
- `/swagger-ui.html`
- `/api-docs`

### Logging

Logging is configured in `logback-spring.xml` with different levels for development and production.

### External Services

- Mailgun: Used for email services
- Orquestas de Galicia API: External API integration

### Code Style

- Use Lombok annotations to reduce boilerplate code
- Use MapStruct for object mapping
- Follow Spring Boot conventions for service and repository implementations
- Use JPA annotations for entity definitions

### Common Issues

- Ensure Java 17 is installed and configured correctly
- Check database connection settings if experiencing connection issues
- Verify environment variables are set correctly for external services