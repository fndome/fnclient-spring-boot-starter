## [fnclient-spring-boot-starter](https://github.com/fndome/fnclient-spring-boot-starter)

HTTP Exchange Client for Spring Boot - 支持本地/远程混合调用模式

### 特性

- 基于 Spring Web Service 的 `@HttpExchange` 注解
- 支持 `standalone` 模式，可将远程调用路由到本地 Controller
- 混合模式：优先使用 standalone，未覆盖的 remote 自动使用 HTTP 代理
- 自定义异常类 `RemoteCallException`

### Maven 依赖

```xml
<dependency>
    <groupId>me.fndo</groupId>
    <artifactId>fnclient-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 快速开始

#### 1. 定义 Remote 接口

```java
@HttpExchange("http://shop/shop")
public interface ShopRemote {

    @PutExchange("/pay-to-self/cancel")
    HandledOnly cancelPayToSelf();

    @GetExchange("/public/get/{shopId}")
    ShopDto get(@PathVariable("shopId") Long shopId);
}
```

#### 2. 本地 Controller（可选，用于 standalone 模式）

```java
@RestController
@RequestMapping("/shop")
@Standalone(remote = ShopRemote.class)
public class ShopController {

    @PutMapping("/pay-to-self/cancel")
    public HandledOnly cancelPayToSelf() {
        // 本地实现
        return HandledOnly.of(true);
    }

    @GetMapping("/public/get/{shopId}")
    public ShopDto get(@PathVariable("shopId") Long shopId) {
        // 本地实现
        return new ShopDto();
    }
}
```

#### 3. 启用 HttpExchange

```java
@SpringBootApplication
@EnableHttpExchange(
    basePackages = {"io.fndo.remote"},           // remote 接口所在包
    standalonePackages = {"io.fndo.controller"}  // Controller 所在包（可选）
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 4. 使用 Remote

```java
@Service
public class OrderService {

    @Resource
    private ShopRemote shopRemote;

    public void doSomething() {
        // 如果配置了 standalone，会调用本地 Controller
        // 否则会通过 HTTP 调用远程服务
        ShopDto shop = shopRemote.get(1L);
    }
}
```

### 工作模式

| 配置 | 行为 |
|------|------|
| 仅 `basePackages` | 所有 Remote 使用 HTTP 远程调用 |
| `basePackages` + `standalonePackages` | 被 `@Standalone` 覆盖的 Remote 使用本地调用，其余使用 HTTP 远程调用 |

### 异常处理

远程调用失败时根据 HTTP 状态码抛出不同的异常：

```java
try {
    shopRemote.get(1L);
} catch (Remote4xxException e) {
    // 客户端错误：400-499
    int statusCode = e.getStatusCode();  // 状态码
    String uri = e.getUri();             // 请求 URI
    String body = e.getBody();           // 响应体
} catch (Remote5xxException e) {
    // 服务器错误：500-599
} catch (Remote3xxException e) {
    // 重定向：300-399
} catch (Remote1xxException e) {
    // 信息性响应：100-199
}
```

**异常类层次**：
- `RemoteCallException` (基类)
  - `Remote1xxException` (信息性响应)
  - `Remote3xxException` (重定向)
  - `Remote4xxException` (客户端错误)
  - `Remote5xxException` (服务器错误)

### 版本要求

- Java 17+
- Spring Boot 3.2.6+
- Spring Framework 6.1.8+

### 项目结构

```
me.fndo.fnclient/
├── EnableHttpExchange.java         # 主注解
├── Standalone.java                 # Standalone 注解
├── HttpExchangeRegistrar.java      # Bean 注册器
├── StandaloneFactoryBean.java      # Standalone 工厂
├── StandaloneMethodInterceptor.java # 方法拦截器
├── config/
│   └── HttpBeanFactory.java        # HTTP 代理工厂
├── exception/
│   └── RemoteCallException.java    # 自定义异常
└── util/
    └── ClassFileReader.java        # 类扫描工具
```
