# 结构化日志断言框架

Pair-wise GSB 标注任务仓库（第 17 批 / 282）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的日志是结构化的，测试里想断言"某个请求产生了包含指定字段的错误日志"，但现在是字符串匹配。请从零实现一个结构化日志断言框架。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持捕获结构化日志条目（级别、时间、消息、键值字段、异常信息）；2) 支持断言条件：按级别、按键值字段匹配、按字段存在性、按异常类型组合查询；3) 支持顺序断言：要求日志按指定顺序出现，且允许中间插入无关日志；4) 支持数量断言：某类日志出现恰好 N 次或至少 N 次，失败时输出实际匹配到的条目；5) 支持上下文关联：按请求标识或链路标识过滤出属于同一请求的日志再断言；6) 支持失败信息可读：断言失败时输出实际的日志列表与不匹配原因，避免只显示布尔值；7) 提供统计：捕获条目数、断言次数与失败次数；8) 测试覆盖字段匹配、顺序断言、数量断言、上下文过滤、失败信息与统计；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。

## 结构化日志断言框架（本仓库实现）

核心类位于 `com.example.logassert`：

- `LogCapture`：线程安全的结构化日志捕获器，记录 `LogEntry`（级别、时间、消息、键值字段、异常）。
- `StructuredLogger`：把带键值对的日志（可注入 `Clock`）写入捕获器。
- `LogMatcher`：组合查询条件——级别（`error()`/`warn()`/`info()`）、键值字段（`withField`）、字段存在性（`withFieldPresent`）、消息包含（`withMessageContaining`）、异常类型（`withException(Class)`）。
- `LogAssertions.assertThat(capture)`：
  - `hasEntryMatching(m)` / `hasNoEntryMatching(m)`；
  - `containsInOrder(m1, m2, ...)`：顺序断言，允许中间插入无关日志；
  - `matching(m).occursExactly(n)` / `occursAtLeast(n)`：数量断言，失败时打印实际匹配到的条目；
  - `capture.forRequest(reqId)` / `forTrace(traceId)` / `forContext(key, value)`：按请求/链路标识过滤后再断言。
- 失败时输出实际日志列表（时间戳、级别、字段、异常）与每条条目的不匹配原因；`LogStats` 提供捕获数、断言数、失败数统计。

示例：

```java
LogCapture capture = new LogCapture();
StructuredLogger log = new StructuredLogger(capture);

log.info("checkout started", "requestId", "req-1");
log.error("payment failed", new PaymentException("declined"),
        "requestId", "req-1", "errorCode", "PAYMENT_DECLINED", "amount", 9900);

LogAssertions.assertThat(capture.forRequest("req-1")).containsInOrder(
        LogMatcher.info().withMessageContaining("checkout started"),
        LogMatcher.error().withField("errorCode", "PAYMENT_DECLINED")
                .withFieldPresent("amount").withException(PaymentException.class));
LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursExactly(1);
```

测试见 `src/test/java/com/example/logassert`（37 个用例，覆盖字段匹配、顺序、数量、上下文过滤、失败信息与统计）。
