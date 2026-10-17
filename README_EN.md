# result-spring-boot-starter

A Spring Boot unified response starter: add one dependency and every Controller return value is automatically wrapped in a consistent envelope, while global exceptions are converged into a unified error format.

中文 | [English](README_EN.md)

## Introduction

When every endpoint invents its own response shape (`{"success":true}`, `{"code":0}`, bare objects...), the frontend has to write parsing branches for each variant; exception handling scattered across Controllers leaves error formats and HTTP status semantics inconsistent. This starter converges the response surface into two things:

1. **Unified wrapping** — a `ResponseBodyAdvice` intercepts Controller return values and wraps them into `{"code":200,"message":"OK","timestamp":"...","data":...}`;
2. **Unified exceptions** — a `@ControllerAdvice` globally captures business exceptions, parameter validation exceptions, HTTP semantic exceptions, and unknown exceptions, all returned in the same envelope.

Business code only writes normal logic and `BaseException`; response formats and error codes stop being reinvented per endpoint.

## Features

- **Zero-intrusion wrapping** — works out of the box; Controllers need no changes; `BaseResult` and `ResponseEntity` return types skip wrapping automatically
- **Annotation escape hatch** — `@IgnoreUnifiedResult` skips wrapping at method level (file downloads, third-party callbacks, health checks)
- **Configuration-level filtering** — `worm.result.filter-unified-result-methods` filters by `ClassName.methodName` (case-insensitive), no code changes required
- **Global exception matrix** — business exceptions (custom codes) / @Valid validation / type mismatch / 405 / 415 / 413 / 404 / 500 fallback, all covered; field-level errors returned as `Map<field, message>`
- **HTTP 200 envelope semantics** — errors also return HTTP 200 with the business code in the body's `code` field, avoiding non-2xx interception by proxies/gateways/browsers
- **i18n for String returns** — String return values are resolved through `MessageSource` before wrapping, and correctly serialized to a JSON string (working around the StringHttpMessageConverter type-cast issue)
- **Overridable** — every bean is `@ConditionalOnMissingBean`; register your own implementation to override; both switches can be disabled independently

## Requirements

| Dependency | Version |
| --- | --- |
| JDK | 8+ |
| Maven | 3.6+ |
| Spring Boot | 2.7.x |
| Swagger annotations | Optional (`swagger-annotations`, only for `@Schema` documentation) |

This repository targets the JDK 8 / Spring Boot 2.7 baseline; build output is Java 8 bytecode, ready to use in Spring Boot 2.x projects.

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.worm</groupId>
    <artifactId>result-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

No configuration needed — write a plain Controller and see the effect:

```java
@RestController
public class OrderController {

    @GetMapping("/order/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id); // automatically wrapped in BaseResult
    }
}
```

Actual response:

```json
{
  "code": 200,
  "message": "OK",
  "timestamp": "2026-10-17 12:00:00",
  "data": { "id": 1, "status": "PENDING" }
}
```

### 2. Throw a business exception

```java
@GetMapping("/order/{id}")
public Order getById(@PathVariable Long id) {
    Order order = orderService.getById(id);
    if (order == null) {
        throw new BaseException(4004, "Order not found");
    }
    return order;
}
```

Response (HTTP 200, envelope carries the business code):

```json
{
  "code": 4004,
  "message": "FAIL",
  "timestamp": "2026-10-17 12:00:00",
  "data": "Order not found"
}
```

### 3. Skip wrapping

Method-level annotation (for individual methods):

```java
@IgnoreUnifiedResult
@GetMapping("/download")
public void download(HttpServletResponse response) {
    // file download, return value is not wrapped
}
```

Configuration-level filtering (for a batch of methods, case-insensitive, format `ClassName.methodName`):

```yaml
worm:
  result:
    filter-unified-result-methods:
      - CheckTokenEndpoint.checkToken
      - HealthController.ping
```

### 4. Disable the switches

```yaml
worm:
  result:
    enable-unified-result: false    # disable unified wrapping (default true)
    enable-unified-exception: false # disable unified exception handling (default true)
```

### 5. Custom override

Register your own bean to override the auto-configuration (`@ConditionalOnMissingBean` semantics):

```java
@Bean
public BaseResultHandler myResultHandler(BaseResultProperties properties,
                                         ObjectProvider<MessageSource> messageSourceProvider) {
    return new BaseResultHandler(properties, messageSourceProvider);
}
```

## Response Format

| Field | Type | Description |
| --- | --- | --- |
| `code` | int | HTTP-status-style success/failure code (200/400/401/403/404/500, or custom) |
| `bizCode` | String | Optional business semantic code; reserved field, not set for regular responses |
| `message` | String | Status description: `OK` / `FAIL` / `UNAUTHORIZED` / `FORBIDDEN` / `NOT_FOUND` / `INTERNAL_ERROR`, or custom |
| `timestamp` | String | Server response time, format `yyyy-MM-dd HH:mm:ss` |
| `data` | T | Business data; on exceptions, the error message or a field-level error `Map` |

`BaseResult` factory methods: `ok()` / `ok(data)` / `ok(message, data)` / four `fail(...)` overloads / `unauthorized()` / `forbidden()` / `notFound()` / `error()` / `custom(code, message, data)`, plus `writeJson(response, result)` for writing the response stream directly.

## Exception Handling Matrix

| Exception | code | data |
| --- | --- | --- |
| `BaseException` | Custom code from the exception | Exception message |
| `MethodArgumentNotValidException` / `BindException` | 400 | `Map<field, validation message>` |
| `ConstraintViolationException` | 400 | `Map<field, validation message>` (method-name prefix stripped) |
| `MissingServletRequestParameterException` | 400 | Missing parameter hint |
| `MethodArgumentTypeMismatchException` | 400 | Parameter name and expected type hint |
| `HttpRequestMethodNotSupportedException` | 405 | Unsupported method hint |
| `HttpMediaTypeNotSupportedException` | 415 | Unsupported media type hint |
| `MaxUploadSizeExceededException` | 413 | Upload size exceeded hint |
| `NoHandlerFoundException` | 404 | Request path hint |
| `Throwable` (fallback) | 500 | Exception message ("系统内部错误" when null) |

## Full Configuration List

Prefix `worm.result`:

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enable-unified-result` | boolean | `true` | Enable unified response wrapping |
| `enable-unified-exception` | boolean | `true` | Enable unified exception handling |
| `filter-unified-result-methods` | List\<String\> | empty | Methods to skip wrapping, format `ClassName.methodName`, must contain exactly one `.` |

## Architecture and Package Structure

```text
com.worm
├── config
│   └── BaseResultAutoConfiguration   // The sole auto-configuration class (registered via AutoConfiguration.imports)
├── properties
│   └── BaseResultProperties          // Configuration properties (prefix worm.result)
└── unified
    ├── annotation
    │   └── IgnoreUnifiedResult       // Method-level skip-wrapping annotation
    ├── enums
    │   └── EnumBaseResult            // Status description enum (OK/FAIL/...)
    ├── exp
    │   ├── BaseException             // Business exception (carries a custom error code)
    │   └── BaseExceptionHandler      // Global exception handler
    └── result
        ├── BaseResult                // Unified response body + factory methods
        └── BaseResultHandler         // ResponseBodyAdvice wrapping handler
```

Auto-configured bean list (`BaseResultAutoConfiguration`; business code can override conditionally):

| Bean | Registration condition | Description |
| --- | --- | --- |
| `BaseResultHandler` | When missing and `enable-unified-result=true` | Unified return value wrapping |
| `BaseExceptionHandler` | When missing and `enable-unified-exception=true` | Unified global exception handling |
| `BaseResultProperties` | Unconditional | Configuration property binding |

## FAQ

### 1. Why do errors also return HTTP 200?

Error codes live in the body's `code` field instead of the HTTP status code, so that proxies, gateways, and browsers don't intercept/redirect based on HTTP status, letting the frontend handle success and failure in one parsing path. For scenarios that need standard HTTP semantics (e.g., external REST APIs), override `BaseExceptionHandler` to return a `ResponseEntity`.

### 2. What happens when `MessageSource` is absent?

`BaseResultHandler` obtains `MessageSource` lazily via `ObjectProvider`; when absent from the container, String return values gracefully fall back to the raw value, leaving the wrapping main path unaffected (slim containers without a web environment also start normally).

### 3. What happens to `BaseResult` / `ResponseEntity` return values?

They are not re-wrapped and are returned as-is. `BaseResult` leaves full control of the response to business code; the semantics of `ResponseEntity` (status code, headers) are likewise fully preserved.

### 4. Why is a String return value serialized to a JSON string?

When a Controller declares a String return type, Spring has already selected `StringHttpMessageConverter`; returning a `BaseResult` object directly would trigger a `ClassCastException`. The starter serializes the wrapped result to a JSON string first and hands it back to the converter, so the frontend still receives the standard envelope structure.

### 5. How do I use this with Spring Boot 3 / the jakarta namespace?

This repository is the open-source JDK 8 / Spring Boot 2.7 (javax) baseline. For the jakarta baseline (Spring Boot 3+ / JDK 17), refer to the internally maintained version, or change this repository's `javax.*` references to `jakarta.*` and upgrade to `spring-boot-dependencies:3.x`.

## License

This project is licensed under the [Apache License 2.0](LICENSE). See the [LICENSE](LICENSE) file for details.
