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


## 框架用法

```java
try (LogCaptor captor = LogCaptor.start()) {
    service.handle("req-42", "user-7");
    service.failPayment("req-42", "user-7", "insufficient funds");

    LogAssertions.assertThat(captor)
            .withContext("requestId", "req-42")          // 按请求/链路标识过滤
            .inOrder(                                     // 顺序断言（允许中间插入无关日志）
                    messageContains("received"),
                    level(Level.ERROR))
            .exists(level(Level.ERROR)                    // 组合条件
                    .and(field("userId", "user-7"))
                    .and(hasField("reason"))
                    .and(exceptionIs(IllegalStateException.class)))
            .exactly(1, level(Level.ERROR));              // 数量断言

    AssertionStats stats = captor.stats();                // 捕获数 / 断言数 / 失败数
}
```

断言失败时抛出 `AssertionError`，消息中包含失败原因、实际匹配到的条目列表
以及当前作用域内的全部日志条目（含字段与异常堆栈摘要），而不是一个布尔值。

主要组件（`com.example.logassert`）：

| 类 | 职责 |
|----|------|
| `StructuredLogger` / `LogEntry` / `Level` | 结构化日志核心：级别、时间、消息、键值字段、异常 |
| `LogCaptor` | 捕获会话内全部日志条目（try-with-resources） |
| `LogCondition` / `Conditions` | 条件 DSL：级别、字段值、字段存在性、异常类型、消息，可 `and` / `negate` 组合 |
| `LogAssertion` / `LogAssertions` | 断言入口：`exists` / `absent` / `exactly` / `atLeast` / `inOrder` / `withContext` / `filter` |
| `AssertionStats` | 统计：captured / asserted / failed |

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的日志是结构化的，测试里想断言"某个请求产生了包含指定字段的错误日志"，但现在是字符串匹配。请从零实现一个结构化日志断言框架。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持捕获结构化日志条目（级别、时间、消息、键值字段、异常信息）；2) 支持断言条件：按级别、按键值字段匹配、按字段存在性、按异常类型组合查询；3) 支持顺序断言：要求日志按指定顺序出现，且允许中间插入无关日志；4) 支持数量断言：某类日志出现恰好 N 次或至少 N 次，失败时输出实际匹配到的条目；5) 支持上下文关联：按请求标识或链路标识过滤出属于同一请求的日志再断言；6) 支持失败信息可读：断言失败时输出实际的日志列表与不匹配原因，避免只显示布尔值；7) 提供统计：捕获条目数、断言次数与失败次数；8) 测试覆盖字段匹配、顺序断言、数量断言、上下文过滤、失败信息与统计；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
