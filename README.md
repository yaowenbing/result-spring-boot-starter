# result-spring-boot-starter

Spring Boot 统一返回结果 Starter：一行依赖接入，全站 Controller 返回值自动包装为统一信封，全局异常自动收敛为统一错误格式。

[English](README_EN.md) | 中文

## 简介

每个接口各写各的返回结构（`{"success":true}`、`{"code":0}`、裸对象……），前端要为每种形态写解析分支；异常处理散落在各 Controller，错误格式与 HTTP 状态码语义不统一。本 starter 把响应出口收敛为两件事：

1. **统一包装** — `ResponseBodyAdvice` 拦截 Controller 返回值，自动包为 `{"code":200,"message":"OK","timestamp":"...","data":...}`；
2. **统一异常** — `@ControllerAdvice` 全局捕获业务异常、参数校验异常、HTTP 语义异常与未知异常，一律以同一信封返回。

业务代码只写正常逻辑与 `BaseException`，响应格式与错误码从此不再各写各的。

## 特性

- **零侵入包装** — 引入即生效，Controller 无需任何改动；`BaseResult` 与 `ResponseEntity` 返回值自动跳过包装
- **注解逃生门** — `@IgnoreUnifiedResult` 标注方法级跳过包装（文件下载、第三方回调、健康检查）
- **配置级过滤** — `worm.result.filter-unified-result-methods` 按 `类名.方法名`（不区分大小写）过滤，无需改代码
- **全局异常矩阵** — 业务异常（自定义码）/ @Valid 校验 / 类型不匹配 / 405 / 415 / 413 / 404 / 兜底 500 全覆盖，字段级错误以 `Map<字段名, 消息>` 返回
- **HTTP 200 信封语义** — 错误也返回 HTTP 200，业务码在 body 的 `code` 字段，避免非 2xx 状态码被代理/网关/浏览器层拦截
- **String 返回值国际化** — String 类型返回值经 `MessageSource` 查找后包装，并正确序列化为 JSON 字符串（规避 StringHttpMessageConverter 类型转换异常）
- **可覆盖** — 所有 Bean 均 `@ConditionalOnMissingBean`，自定义实现注册即覆盖；两个开关可独立关闭

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 8+ |
| Maven | 3.6+ |
| Spring Boot | 2.7.x |
| Swagger 注解 | 可选（`swagger-annotations`，仅用于 `@Schema` 文档描述） |

本仓库为 JDK 8 / Spring Boot 2.7 基线，编译产物为 Java 8 字节码，可直接用于 Spring Boot 2.x 项目。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.worm</groupId>
    <artifactId>result-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

引入后无需任何配置，写一个普通 Controller 即可看到效果：

```java
@RestController
public class OrderController {

    @GetMapping("/order/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id); // 自动包装为 BaseResult
    }
}
```

实际响应：

```json
{
  "code": 200,
  "message": "OK",
  "timestamp": "2026-10-17 12:00:00",
  "data": { "id": 1, "status": "PENDING" }
}
```

### 2. 抛出业务异常

```java
@GetMapping("/order/{id}")
public Order getById(@PathVariable Long id) {
    Order order = orderService.getById(id);
    if (order == null) {
        throw new BaseException(4004, "订单不存在");
    }
    return order;
}
```

响应（HTTP 200，信封携带业务码）：

```json
{
  "code": 4004,
  "message": "FAIL",
  "timestamp": "2026-10-17 12:00:00",
  "data": "订单不存在"
}
```

### 3. 跳过包装

方法级注解（适合单个方法）：

```java
@IgnoreUnifiedResult
@GetMapping("/download")
public void download(HttpServletResponse response) {
    // 文件下载，返回值不被包装
}
```

配置级过滤（适合一批方法，不区分大小写，格式 `类名.方法名`）：

```yaml
worm:
  result:
    filter-unified-result-methods:
      - CheckTokenEndpoint.checkToken
      - HealthController.ping
```

### 4. 关闭开关

```yaml
worm:
  result:
    enable-unified-result: false    # 关闭统一包装（默认 true）
    enable-unified-exception: false # 关闭统一异常处理（默认 true）
```

### 5. 自定义覆盖

注册自己的 Bean 即可覆盖自动配置（`@ConditionalOnMissingBean` 语义）：

```java
@Bean
public BaseResultHandler myResultHandler(BaseResultProperties properties,
                                         ObjectProvider<MessageSource> messageSourceProvider) {
    return new BaseResultHandler(properties, messageSourceProvider);
}
```

## 响应格式

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | HTTP 状态码语义的成功/失败码（200/400/401/403/404/500，或自定义） |
| `bizCode` | String | 可选业务语义码，预留字段，常规响应不设置 |
| `message` | String | 状态说明：`OK` / `FAIL` / `UNAUTHORIZED` / `FORBIDDEN` / `NOT_FOUND` / `INTERNAL_ERROR`，或自定义 |
| `timestamp` | String | 服务端响应时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `data` | T | 业务数据；异常场景为错误消息或字段级错误 `Map` |

`BaseResult` 提供的工厂方法：`ok()` / `ok(data)` / `ok(message, data)` / `fail(...)` 四种重载 / `unauthorized()` / `forbidden()` / `notFound()` / `error()` / `custom(code, message, data)`，以及直接写响应流的 `writeJson(response, result)`。

## 异常处理矩阵

| 异常 | code | data |
| --- | --- | --- |
| `BaseException` | 异常自定义码 | 异常消息 |
| `MethodArgumentNotValidException` / `BindException` | 400 | `Map<字段名, 校验消息>` |
| `ConstraintViolationException` | 400 | `Map<字段名, 校验消息>`（去方法名前缀） |
| `MissingServletRequestParameterException` | 400 | 缺少的参数提示 |
| `MethodArgumentTypeMismatchException` | 400 | 参数名与期望类型提示 |
| `HttpRequestMethodNotSupportedException` | 405 | 不支持的方法提示 |
| `HttpMediaTypeNotSupportedException` | 415 | 不支持的媒体类型提示 |
| `MaxUploadSizeExceededException` | 413 | 上传大小超限提示 |
| `NoHandlerFoundException` | 404 | 请求路径提示 |
| `Throwable`（兜底） | 500 | 异常消息（为空时「系统内部错误」） |

## 配置项完整列表

前缀 `worm.result`：

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enable-unified-result` | boolean | `true` | 是否启用统一返回包装 |
| `enable-unified-exception` | boolean | `true` | 是否启用统一异常处理 |
| `filter-unified-result-methods` | List\<String\> | 空 | 跳过包装的方法列表，格式 `类名.方法名`，须恰好包含一个 `.` |

## 架构与包结构

```text
com.worm
├── config
│   └── BaseResultAutoConfiguration   // 唯一自动配置类（经 AutoConfiguration.imports 注册）
├── properties
│   └── BaseResultProperties          // 配置属性（前缀 worm.result）
└── unified
    ├── annotation
    │   └── IgnoreUnifiedResult       // 方法级跳过包装注解
    ├── enums
    │   └── EnumBaseResult            // 状态说明枚举（OK/FAIL/...）
    ├── exp
    │   ├── BaseException             // 业务异常（携带自定义错误码）
    │   └── BaseExceptionHandler      // 全局异常处理器
    └── result
        ├── BaseResult                // 统一返回体 + 工厂方法
        └── BaseResultHandler         // ResponseBodyAdvice 包装处理器
```

自动装配的 Bean 清单（`BaseResultAutoConfiguration`，业务方可按条件覆盖）：

| Bean | 注册条件 | 说明 |
| --- | --- | --- |
| `BaseResultHandler` | 缺失时且 `enable-unified-result=true` | 返回值统一包装 |
| `BaseExceptionHandler` | 缺失时且 `enable-unified-exception=true` | 全局异常统一处理 |
| `BaseResultProperties` | 无条件 | 配置属性绑定 |

## FAQ

### 1. 为什么错误也返回 HTTP 200？

错误码放在 body 的 `code` 字段而非 HTTP 状态码，是为了避免代理、网关、浏览器层按 HTTP 状态码做拦截/重定向，让前端在同一套解析逻辑里处理成功与失败。需要标准 HTTP 语义的场景（如对外的 REST API），可自行覆盖 `BaseExceptionHandler` 返回 `ResponseEntity`。

### 2. `MessageSource` 不存在时会怎样？

`BaseResultHandler` 经 `ObjectProvider` 延迟获取 `MessageSource`，容器中缺失时 String 返回值自动退化为使用原始值，不影响统一包装主流程（无 Web 的精简容器也能正常启动）。

### 3. 已经是 `BaseResult` / `ResponseEntity` 的返回值会怎样？

不重复包装，原样返回。`BaseResult` 交由业务完全掌控响应内容；`ResponseEntity` 的语义（状态码、响应头）同样完整保留。

### 4. String 返回值为什么序列化成 JSON 字符串？

Controller 声明返回 String 时，Spring 已选定 `StringHttpMessageConverter`，若直接返回 `BaseResult` 对象会触发 `ClassCastException`。starter 先将包装结果序列化为 JSON 字符串再交还转换器，前端收到的仍是标准信封结构。

### 5. 如何接入 Spring Boot 3 / jakarta 命名空间版本？

本仓库为 JDK 8 / Spring Boot 2.7（javax）基线的开源版本。jakarta 基线（Spring Boot 3+/JDK 17）请参考内部维护版本，或将本仓库的 `javax.*` 引用改为 `jakarta.*` 并升级至 `spring-boot-dependencies:3.x`。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源，详见 [LICENSE](LICENSE) 文件。
