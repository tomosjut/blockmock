# 🤝 Contributing to BlockMock

Thank you for your interest in contributing to BlockMock! We appreciate all contributions, whether it's bug fixes, new features, documentation improvements, or reporting issues.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Testing](#testing)

## 📜 Code of Conduct

This project follows standard open source etiquette:
- Be respectful to other contributors
- Focus on constructive feedback
- Accept different perspectives and experiences

## 🚀 How Can I Contribute?

### 🐛 Bug Reports

Found a bug? Help us by:

1. **Check first** if the bug has already been [reported](https://github.com/tomosjut/blockmock/issues)
2. **Create a new issue** with:
   - A clear title
   - Description of the problem
   - Steps to reproduce
   - Expected vs. actual behavior
   - Your environment (OS, Java version, etc.)
   - Screenshots if relevant

### 💡 Feature Requests

Have an idea for a new feature?

1. **Check first** the [existing feature requests](https://github.com/tomosjut/blockmock/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement)
2. **Create an issue** with:
   - Clear description of the feature
   - Use case / why this feature would be useful
   - Example of how it would work

### 🔧 Code Contributions

Want to contribute code? Great! Follow these steps:

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a branch** for your changes: `git checkout -b feature/my-feature` or `fix/bug-name`
4. **Make your changes** (see [Development Setup](#development-setup))
5. **Test your changes** thoroughly
6. **Commit** with clear messages (see [Commit Guidelines](#commit-guidelines))
7. **Push** to your fork
8. **Create a Pull Request**

## 🛠️ Development Setup

### Prerequisites

- **Java 21+** (we use Quarkus 3.17.4)
- **Maven 3.8+**
- **Docker & Docker Compose** OR **Podman & podman-compose**
- **Git**

### Local Development Environment

```bash
# 1. Fork and clone the repository
git clone https://github.com/your-username/blockmock.git
cd blockmock

# 2. Start PostgreSQL database
docker-compose up -d
# OR with Podman:
podman-compose up -d

# 3. Start BlockMock in development mode
mvn quarkus:dev

# The application is now available at http://localhost:8080
```

### Project Structure

```
blockmock/
├── src/
│   ├── main/
│   │   ├── java/nl/blockmock/
│   │   │   ├── domain/          # JPA entities
│   │   │   ├── protocol/        # Protocol implementations (HTTP, SFTP, AMQP, etc.)
│   │   │   ├── service/         # Business logic
│   │   │   └── resource/        # REST API endpoints
│   │   └── resources/
│   │       └── META-INF/resources/
│   │           ├── index.html   # Frontend HTML
│   │           ├── css/         # Styling
│   │           └── js/app.js    # Frontend JavaScript
│   └── test/                    # Unit & integration tests
├── demo/                        # Demo endpoints and scripts
├── docker-compose.yml           # PostgreSQL setup
└── start.sh / stop.sh          # Helper scripts
```

### Hot Reload

Quarkus dev mode supports hot reload:
- **Java changes**: Automatically recompiled on request
- **Frontend changes** (HTML/CSS/JS): Refresh browser
- **Database schema**: Automatically updated via Hibernate

## 🔀 Pull Request Process

### Before Creating a PR

- [ ] Code compiles without errors: `mvn clean compile`
- [ ] Tests pass: `mvn test`
- [ ] Code follows the [Coding Standards](#coding-standards)
- [ ] You've written meaningful commit messages
- [ ] You've updated documentation if needed

### PR Template

Use this template for your Pull Request:

```markdown
## Description
[Brief description of what you changed and why]

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would break existing functionality)
- [ ] Documentation update

## Related Issues
Fixes #[issue number]

## Testing
[Describe how you tested the changes]

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code compiles without warnings
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Commit messages follow guidelines
```

### Review Process

1. Maintainer reviews your PR within 3-5 days
2. Feedback is provided if needed
3. You address the feedback
4. PR gets merged!

## 📏 Coding Standards

### Java

- **Java 21** features can be used
- **Lombok**: Use `@Getter`, `@Setter`, etc. for boilerplate reduction
- **Naming**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Formatting**: 4 spaces indent, no tabs
- **Comments**: JavaDoc for public methods

```java
/**
 * Retrieves mock endpoint by ID.
 *
 * @param id The endpoint ID
 * @return Optional containing the endpoint if found
 */
public Optional<MockEndpoint> findById(Long id) {
    return Optional.ofNullable(MockEndpoint.findById(id));
}
```

### JavaScript (Frontend)

- **Vanilla JS**: No frameworks (we keep it simple!)
- **ES6+**: Use modern syntax (arrow functions, template literals, etc.)
- **Naming**: Same as Java (`camelCase` for functions/variables)
- **Async**: Use `async/await` for API calls
- **Formatting**: 4 spaces indent

```javascript
async function loadEndpoints() {
    try {
        const response = await fetch('/api/endpoints');
        const endpoints = await response.json();
        renderEndpoints(endpoints);
    } catch (error) {
        console.error('Failed to load endpoints:', error);
    }
}
```

### Database

- **JPA/Hibernate**: For database interactions
- **Migrations**: Hibernate auto-DDL in dev, Flyway/Liquibase for production (future)
- **Column naming**: `snake_case` in database, `camelCase` in Java

## 📝 Commit Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) convention:

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no functionality)
- `refactor`: Code refactoring
- `test`: Test additions/changes
- `chore`: Build, configuration, dependencies

### Examples

```bash
# Feature
feat(http): add support for request body matching in responses

# Bug fix
fix(sftp): resolve port conflict on startup

# Documentation
docs: update CONTRIBUTING.md with testing guidelines

# Breaking change
feat(api)!: redesign endpoint response structure

BREAKING CHANGE: Response format changed from {data} to {result}
```

### Issue References

Link your commit to an issue:

```bash
git commit -m "fix(ui): resolve save button validation issue

Fixes #42"
```

## 🧪 Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=HttpMockServiceTest

# Run specific test method
mvn test -Dtest=HttpMockServiceTest#testBodyMatching
```

### Integration Tests

```bash
# Run integration tests (PostgreSQL must be running)
mvn verify
```

### Manual Testing

1. Start the application: `./start.sh`
2. Open http://localhost:8080
3. Test your changes via the UI
4. Check the Request Logs for debugging

### Test Coverage

We aim for **>70% code coverage** for new features.

```bash
# Generate coverage report
mvn jacoco:report

# Open report
open target/site/jacoco/index.html
```

## 🎨 UI/Frontend Contributions

For frontend changes:

1. **Test in multiple browsers**: Chrome, Firefox, Safari
2. **Responsive design**: Test on different screen sizes
3. **Accessibility**: Use semantic HTML, ARIA labels where needed
4. **Consistency**: Follow existing UI patterns and styling

## 📚 Documentation

Documentation is as important as code!

- **Code comments**: For complex logic
- **JavaDoc**: For public APIs
- **README updates**: For new features
- **Demos**: Add demo endpoints in `demo/` if relevant

## ❓ Questions?

Have questions about contributing?

- 💬 Open a [Discussion](https://github.com/tomosjut/blockmock/discussions)
- 📧 Contact via issues
- 🐛 Check [existing issues](https://github.com/tomosjut/blockmock/issues)

## 🙏 Thank You!

Every contribution, no matter how small, is appreciated. Together we make BlockMock better! 🚀

---

**Happy Coding!** 🧱✨
