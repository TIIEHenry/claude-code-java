# Claude Code Java 知识文档

本文档记录移植过程中的技术知识、设计决策和实现细节。

---

## 1. Reactor Flux 响应式编程

### 1.1 什么是 Flux

Flux 是 **Project Reactor** 库的核心类，实现了 **Reactive Streams** 规范。它是 Java 中的响应式编程抽象，代表一个 **0 到 N 个元素的异步序列**。

```
Flux<T> = 异步产生 0~N 个 T 元素的流
Mono<T> = 异步产生 0~1 个 T 元素的流 (Flux 的特例)
```

### 1.2 核心概念对比

| 传统方式 | 响应式方式 |
|---------|-----------|
| `List<T>` 同步集合 | `Flux<T>` 异步流 |
| `T` 同步返回值 | `Mono<T>` 异步返回 |
| 阻塞等待结果 | 订阅后回调通知 |
| `Future.get()` 阻塞 | `Flux.subscribe()` 非阻塞 |

### 1.3 执行模型

```
┌─────────────────────────────────────────────────────────┐
│                     Flux 执行流程                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   创建阶段 (Cold - 不执行)                               │
│   ┌──────────┐                                         │
│   │ Flux.just │  →  不执行，只是定义流                   │
│   │ Flux.from │                                         │
│   │ Flux.defer│                                         │
│   └──────────┘                                         │
│         │                                               │
│         ▼                                               │
│   组装阶段 (Operators)                                  │
│   ┌──────────┐                                         │
│   │ .map()   │  →  定义转换，但不执行                    │
│   │ .filter()│                                         │
│   │ .flatMap()│                                        │
│   └──────────┘                                         │
│         │                                               │
│         ▼                                               │
│   订阅阶段 (Subscribe - 触发执行)                       │
│   ┌──────────┐                                         │
│   │.subscribe│  →  触发执行！开始产生数据                │
│   │(consumer)│                                         │
│   └──────────┘                                         │
│         │                                               │
│         ▼                                               │
│   执行阶段                                              │
│   ┌──────────────────────────────────────┐             │
│   │ Publisher → Subscriber.onNext(data)  │             │
│   │           → Subscriber.onComplete()  │             │
│   │           → Subscriber.onError(e)    │             │
│   └──────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────┘
```

### 1.4 创建 Flux

```java
// 从静态值创建
Flux<String> flux1 = Flux.just("A", "B", "C");

// 从集合创建
Flux<Integer> flux2 = Flux.fromIterable(List.of(1, 2, 3));

// 从数组创建
Flux<Integer> flux3 = Flux.fromArray(new Integer[]{1, 2, 3});

// 空的 Flux
Flux<Void> empty = Flux.empty();

// 生成序列
Flux<Integer> range = Flux.range(1, 10);  // 1..10

// 异步创建
Flux<String> async = Flux.create(sink -> {
    sink.next("A");
    sink.next("B");
    sink.complete();
});

// 延迟创建 (每次订阅时重新计算) - 重要！
Flux<String> deferred = Flux.defer(() -> Flux.just(System.currentTimeMillis() + ""));
```

### 1.5 订阅 Flux

```java
Flux<String> flux = Flux.just("A", "B", "C");

// 最简单的订阅
flux.subscribe(System.out::println);

// 完整订阅 (值、错误、完成)
flux.subscribe(
    value -> System.out.println("Value: " + value),     // onNext
    error -> System.err.println("Error: " + error),     // onError
    () -> System.out.println("Done!")                   // onComplete
);

// 使用 Subscriber (带背压控制)
flux.subscribe(new Subscriber<String>() {
    Subscription subscription;
    
    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        s.request(1);  // 请求 1 个元素 (背压)
    }
    
    @Override
    public void onNext(String s) {
        System.out.println(s);
        subscription.request(1);  // 继续请求下一个
    }
    
    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }
    
    @Override
    public void onComplete() {
        System.out.println("Done");
    }
});
```

### 1.6 常用操作符

```java
Flux.range(1, 10)
    // 转换
    .map(i -> i * 2)                    // 2, 4, 6, 8, ...
    
    // 过滤
    .filter(i -> i > 5)                 // 6, 8, 10, ...
    
    // 扁平映射 (异步操作常用)
    .flatMap(i -> Flux.just(i, i * 10)) // 6, 60, 8, 80, 10, 100, ...
    
    // 合并多个 Flux
    .mergeWith(Flux.just(100, 200))
    
    // 限制
    .take(3)                            // 只取前 3 个
    
    // 错误处理
    .onErrorResume(e -> Flux.just(-1))  // 出错时返回备用值
    
    // 副作用 (用于日志、调试)
    .doOnNext(i -> System.out.println("Processing: " + i))
    .doOnComplete(() -> System.out.println("Done"))
    .doOnError(e -> System.err.println("Error: " + e))
    
    // 订阅触发执行
    .subscribe();
```

### 1.7 背压 (Backpressure)

背压是 Reactive Streams 的核心概念，允许消费者控制生产者的速率：

```java
Flux.range(1, 1000)
    .subscribe(new Subscriber<Integer>() {
        Subscription sub;
        
        @Override
        public void onSubscribe(Subscription s) {
            this.sub = s;
            s.request(5);  // 只请求 5 个，不会收到更多
        }
        
        @Override
        public void onNext(Integer i) {
            System.out.println(i);
            // 处理完再请求下一个
            sub.request(1);
        }
        
        @Override
        public void onError(Throwable t) {}
        
        @Override
        public void onComplete() {}
    });
```

### 1.8 冷流 vs 热流

| 类型 | 特点 | 示例 |
|------|------|------|
| **冷流** | 每次订阅从头开始，订阅者独享数据 | `Flux.just()`, `Flux.fromIterable()` |
| **热流** | 数据独立于订阅者，后订阅者可能错过之前数据 | `Flux.share()`, `Flux.replay()` |

```java
// 冷流示例 - 每次订阅都执行
Flux<String> cold = Flux.defer(() -> {
    System.out.println("Creating...");
    return Flux.just("A", "B");
});
cold.subscribe();  // 打印 "Creating..."
cold.subscribe();  // 再次打印 "Creating..."

// 热流示例 - 共享数据源
Flux<String> hot = Flux.interval(Duration.ofSeconds(1))
    .map(i -> "Tick " + i)
    .share();  // 变成热流
```

### 1.9 阻塞 vs 非阻塞

```java
// ❌ 阻塞方式 (不推荐在生产环境，但 CLI 场景可用)
List<Integer> list = flux.collectList().block();  // 阻塞等待
Integer first = flux.blockFirst();                 // 阻塞获取第一个

// ✅ 非阻塞方式 (推荐)
flux
    .doOnNext(i -> process(i))
    .doOnError(e -> handleError(e))
    .doOnComplete(() -> finish())
    .subscribe();

// 配合 CountDownLatch 等待完成 (CLI 场景常用)
CountDownLatch latch = new CountDownLatch(1);
flux.subscribe(
    System.out::println,
    e -> latch.countDown(),
    () -> latch.countDown()
);
latch.await(30, TimeUnit.SECONDS);  // 超时等待
```

### 1.10 与 TypeScript async generator 对比

| TypeScript | Java Reactor |
|------------|--------------|
| `async function* gen()` | `Flux<T>` |
| `yield value` | `sink.next(value)` |
| `return` | `sink.complete()` |
| `throw error` | `sink.error(error)` |
| `for await (const v of gen())` | `flux.subscribe(v -> ...)` |
| `gen().next()` | `flux.blockFirst()` |

```typescript
// TypeScript async generator
async function* queryLoop(): AsyncGenerator<Message> {
    while (true) {
        const response = await api.query();
        yield response;
        if (response.done) break;
    }
}
```

```java
// Java Flux 等价实现
Flux<Message> queryLoop() {
    return Flux.defer(() -> 
        Flux.create(sink -> {
            CompletableFuture.runAsync(() -> {
                while (!sink.isCancelled()) {
                    ApiResponse response = api.query();
                    sink.next(response);
                    if (response.done) {
                        sink.complete();
                        break;
                    }
                }
            });
        })
    );
}
```

### 1.11 Claude Code Java 中的实际应用

```java
// QueryEngine.java - agentic loop 的 Flux 实现
public Flux<QueryEvent> executeAgenticLoop(String prompt) {
    return Flux.defer(() -> {
        // 延迟创建 - 每次订阅时执行
        interrupted = false;
        return doExecuteLoop(prompt, 0);
    });
}

private Flux<QueryEvent> doExecuteLoop(String prompt, int iteration) {
    if (interrupted || iteration >= maxIterations) {
        return Flux.just(new QueryEvent.Terminal(...));
    }
    
    return executeOneTurn(prompt)
        .expand(event -> {
            if (event instanceof QueryEvent.ToolsComplete) {
                return doExecuteLoop(null, iteration + 1);  // 递归继续
            }
            return Flux.empty();
        })
        .takeUntil(event -> event instanceof QueryEvent.Terminal);
}

// Main.java - 订阅方式
Flux<QueryEvent> eventFlux = queryEngine.executeAgenticLoop(prompt);

CountDownLatch latch = new CountDownLatch(1);
eventFlux.subscribe(
    event -> {
        if (event instanceof QueryEvent.Message msg) {
            printMessage(msg.message());
        }
    },
    error -> {
        System.err.println("Error: " + error.getMessage());
        latch.countDown();
    },
    () -> latch.countDown()
);
latch.await(300, TimeUnit.SECONDS);
```

---

## 2. Sealed Interface (Java 17+)

### 2.1 概念

Sealed interface/class 用于定义**受限的继承层次结构**，明确指定哪些类型可以继承/实现。

### 2.2 语法

```java
// Sealed interface - 明确允许的实现类
public sealed interface QueryEvent permits
    QueryEvent.RequestStart,
    QueryEvent.Message,
    QueryEvent.ToolsExecuting,
    QueryEvent.ToolsComplete,
    QueryEvent.Terminal {
    
    // 接口方法
    default String getTypeName() { ... }
}

// 实现 - 必须是 final、sealed 或 non-sealed
public record RequestStart() implements QueryEvent { }        // final (record 默认 final)
public record Message(Object message) implements QueryEvent { }
public record ToolsExecuting(int toolCount) implements QueryEvent { }
public record ToolsComplete(int resultCount) implements QueryEvent { }
public record Terminal(LoopTransition.Terminal transition) implements QueryEvent { }
```

### 2.3 与 TypeScript union type 对比

```typescript
// TypeScript union type
type QueryEvent = 
  | { type: 'request_start' }
  | { type: 'message'; message: any }
  | { type: 'tools_executing'; toolCount: number }
  | { type: 'tools_complete'; resultCount: number }
  | { type: 'terminal'; transition: TerminalTransition };

// 类型守卫
function getTypeName(event: QueryEvent): string {
    switch (event.type) {
        case 'request_start': return 'request_start';
        case 'message': return 'message';
        // ...
    }
}
```

```java
// Java sealed interface + pattern matching
default String getTypeName() {
    if (this instanceof RequestStart) return "request_start";
    else if (this instanceof Message) return "message";
    else if (this instanceof ToolsExecuting) return "tools_executing";
    // ...
}
```

### 2.4 优势

- **类型安全**: 编译器知道所有可能的子类型
- **穷尽检查**: switch/if-instanceof 可覆盖所有情况
- **模式匹配**: Java 21+ 支持 switch pattern matching

---

## 3. Record 类型 (Java 16+)

### 3.1 概念

Record 是不可变数据载体，自动生成 constructor、getters、equals、hashCode、toString。

### 3.2 语法

```java
// 基本用法
public record QueryState(
    List<Object> messages,
    ToolUseContext toolUseContext,
    int turnCount
) {
    // 自动生成:
    // - QueryState(List, ToolUseContext, int)
    // - messages(), toolUseContext(), turnCount()
    // - equals(), hashCode(), toString()
}

// 使用
QueryState state = new QueryState(messages, context, 1);
List<Object> msgs = state.messages();
```

### 3.3 自定义方法

```java
public record QueryState(
    List<Object> messages,
    ToolUseContext toolUseContext,
    int turnCount
) {
    // 静态工厂方法
    public static QueryState initial(List<Object> messages, ToolUseContext ctx) {
        return new QueryState(messages, ctx, 1);
    }
    
    // 实例方法
    public boolean isEmpty() {
        return messages == null || messages.isEmpty();
    }
    
    // Builder 模式
    public Builder toBuilder() {
        return new Builder(this);
    }
    
    public static class Builder {
        private List<Object> messages;
        private ToolUseContext toolUseContext;
        private int turnCount;
        
        public Builder(QueryState state) {
            this.messages = state.messages();
            this.toolUseContext = state.toolUseContext();
            this.turnCount = state.turnCount();
        }
        
        public Builder messages(List<Object> messages) {
            this.messages = messages;
            return this;
        }
        
        // ... 其他 setter
        
        public QueryState build() {
            return new QueryState(messages, toolUseContext, turnCount);
        }
    }
}
```

### 3.4 与 TypeScript interface 对比

```typescript
// TypeScript interface
interface QueryState {
    messages: any[];
    toolUseContext: ToolUseContext;
    turnCount: number;
}

// 使用
const state: QueryState = { messages: [], toolUseContext: ctx, turnCount: 1 };
```

```java
// Java record
public record QueryState(
    List<Object> messages,
    ToolUseContext toolUseContext,
    int turnCount
) {}

// 使用
QueryState state = new QueryState(List.of(), ctx, 1);
```

---

## 5. Agentic Loop 流式处理核心原理

### 5.1 TypeScript 的真正流式实现

TypeScript 中 `query.ts:659-863` 的核心逻辑：

```typescript
// query.ts - 真正的流式处理
for await (const message of deps.callModel(...)) {
    // ═════════════════════════════════════════════════════
    // ★ 关键：每收到一条消息就立即处理
    // ═════════════════════════════════════════════════════
    
    yield message  // 立即输出给用户
    
    if (message.type === 'assistant') {
        // 1. 收到 tool_use → 立即添加到 executor
        for (const toolBlock of msgToolUseBlocks) {
            streamingToolExecutor.addTool(toolBlock, message)
            // ↑ 工具开始并发执行！不需要等 API 完成
        }
    }
    
    // 2. 同时获取已完成的结果（可能在 streaming 期间已经完成）
    for (const result of streamingToolExecutor.getCompletedResults()) {
        yield result.message
    }
}
```

### 5.2 当前 Java 实现的错误（问题所在）

```java
// QueryEngine.java - 错误的实现
private Flux<QueryEvent> executeOneTurn(String prompt) {
    Flux<Message> responseFlux = session.sendMessageStreaming(prompt);
    
    // ═════════════════════════════════════════════════════
    // ★ 问题：collectList() 等待所有消息完成后才处理
    // ═════════════════════════════════════════════════════
    return responseFlux
        .collectList()  // ← 阻塞！把流式变成了批量
        .flatMapMany(messages -> processMessages(messages));
}

// 工具执行在 API 完成后才开始，完全失去了"边收边执行"的优势
```

### 5.3 时间线对比图

```
TypeScript 时间线 (真正的流式):
────────────────────────────────────────────────────────────────────────→
│ API 流式返回    │ tool_use_1  │ tool_use_2 │ text │ tool_use_3 │ done │
│                 │      ↓      │     ↓      │      │     ↓      │      │
│ Tool 执行       │ Tool1开始   │ Tool2开始  │      │ Tool3开始  │      │
│                 │             │ Tool1完成✓ │      │ Tool2完成✓ │      │
│ 用户看到        │ msg+result1 │ result2    │ text │            │ done │
└────────────────────────────────────────────────────────────────────────→

Java 当前时间线 (假的流式):
────────────────────────────────────────────────────────────────────────→
│ API 流式返回    │ tool_use_1  │ tool_use_2 │ text │ tool_use_3 │ done │
│                 │             │            │      │            │ ↓    │
│ Tool 执行       │             │            │      │            │ 开始 │
│                 │             │            │      │            │ ...  │
│ 用户看到        │             │            │      │            │ 所有 │
└────────────────────────────────────────────────────────────────────────→
```

### 5.4 正确的 Java Flux 实现

应该使用 `.flatMap()` 或 `.handle()` 在每个消息到达时立即处理：

```java
// 正确的实现 - 真正的流式处理
private Flux<QueryEvent> executeOneTurn(String prompt) {
    Flux<Message> responseFlux = session.sendMessageStreaming(prompt);
    
    // 使用 handle() 或 flatMap() 在每个消息到达时立即处理
    return responseFlux
        .handle((message, sink) -> {
            // 1. 立即输出消息
            sink.next(new QueryEvent.Message(message));
            
            // 2. 如果是 assistant 且有 tool_use，立即添加到 executor
            if (message instanceof Message.Assistant assistant) {
                for (ContentBlock block : assistant.content()) {
                    if (block instanceof ContentBlock.ToolUse toolUse) {
                        streamingToolExecutor.addTool(toolUse, assistant);
                        // ↑ 工具立即开始执行！
                    }
                }
            }
            
            // 3. 同时获取已完成的结果
            for (var result : streamingToolExecutor.getCompletedResults()) {
                sink.next(new QueryEvent.Message(result.message()));
            }
        })
        // 流结束后，继续处理剩余的工具结果
        .concatWith(executeRemainingTools());
}
```

### 5.5 Flux 操作符对比

| 操作符 | 行为 | 适用场景 |
|--------|------|----------|
| `.collectList()` | 等待所有元素，返回 List | **错误**：把流式变成批量 |
| `.handle()` | 每个元素到达时处理，可输出多个 | **正确**：流式处理 + 副作用 |
| `.flatMap()` | 每个元素转换为新的 Flux | **正确**：异步操作 |
| `.doOnNext()` | 每个元素到达时执行副作用（不改变流） | 调试/日志 |
| `.buffer()` | 收集 N 个元素后批量处理 | 批量处理（不是流式） |

### 5.6 与 TypeScript async generator 的精确对应

```typescript
// TypeScript
async function* processStream() {
    for await (const msg of apiStream) {
        yield msg;  // 输出
        if (msg.hasToolUse) {
            executor.addTool(msg.tool);  // 副作用
        }
        for (const result of executor.getCompletedResults()) {
            yield result;  // 输出更多
        }
    }
}
```

```java
// Java Flux 等价
Flux<Event> processStream() {
    return apiFlux
        .handle((msg, sink) -> {
            sink.next(msg);  // yield
            if (msg.hasToolUse) {
                executor.addTool(msg.tool);  // 副作用
            }
            for (var result : executor.getCompletedResults()) {
                sink.next(result);  // yield more
            }
        });
}
```

---

## 6. Agentic Loop 流式处理完整解析

### 6.1 整体流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Agentic Loop 完整流程                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   用户输入 prompt                                                            │
│        │                                                                    │
│        ▼                                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    第一轮 (Turn 1)                                   │  │
│   │                                                                     │  │
│   │   ┌─────────────────────────────────────────────────────────────┐  │  │
│   │   │           API Streaming 响应                                  │  │  │
│   │   │                                                             │  │  │
│   │   │   时间线:                                                   │  │  │
│   │   │   0ms  │ 100ms │ 200ms │ 300ms │ 400ms │ 500ms │ done     │  │  │
│   │   │         │       │       │       │       │       │          │  │  │
│   │   │   消息: │text_1│tool_1│text_2│tool_2│text_3│          │  │  │
│   │   │         │       │   ↓   │       │   ↓   │       │          │  │  │
│   │   │   工具: │       │ 开始 │       │ 开始 │       │          │  │  │
│   │   │   执行: │       │       │ 完成 │       │ 完成 │          │  │  │
│   │   │         │       │       │   ↓   │       │   ↓   │          │  │  │
│   │   │   用户: │看到t1│看到t1│看到r1│看到t2│看到r2│看到done │  │  │
│   │   │   看到: │       │+工具1│+结果1│+工具2│+结果2│          │  │  │
│   │   │                                                             │  │  │
│   │   │   ★ 关键：工具在 streaming 期间就开始执行                    │  │  │
│   │   │   ★ 用户边看边等，体验流畅                                    │  │  │
│   │   └─────────────────────────────────────────────────────────────┘  │  │
│   │                                                                     │  │
│   │   API 完成后 → 收集剩余结果 → ToolsComplete 事件                    │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│        │                                                                    │
│        ▼                                                                    │
│   判断：有工具调用？                                                          │
│        │                                                                    │
│        │ 是 → 继续下一轮                                                      │
│        │                                                                    │
│        ▼                                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                    第二轮 (Turn 2)                                   │  │
│   │                                                                     │  │
│   │   用户消息 = [工具结果1, 工具结果2]                                   │  │
│   │   再次调用 API Streaming                                            │  │
│   │   ... 重复上述流程                                                   │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│        │                                                                    │
│        ▼                                                                    │
│   判断：没有工具调用 或 达到最大轮数                                           │
│        │                                                                    │
│        ▼                                                                    │
│   Terminal 事件 → 结束                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Flux 核心概念详解

#### 什么是 Flux？

**Flux = 一个异步的"管道"，数据从一端流入，从另一端流出。**

```
生产者 ──→ [Flux 管道] ──→ 消费者

         ┌─────────────────────────┐
         │                         │
数据源 ──→│  Flux<T>               │──→ 处理数据
         │  (0~N 个 T 元素)        │
         │                         │
         └─────────────────────────┘
```

#### Flux vs 传统 List

```java
// 传统 List - 同步，数据已经在内存中
List<String> list = Arrays.asList("A", "B", "C");
for (String s : list) {
    System.out.println(s);  // 立即打印所有：A B C
}
// 执行顺序：确定、同步、立即完成

// Flux - 异步，数据"慢慢到达"的例子
// ─────────────────────────────────────────────────────────────
// 例1：定时发射 - 每500ms发射一个元素
Flux<Long> flux1 = Flux.interval(Duration.ofMillis(500))
    .take(3);  // 只取前3个

flux1.subscribe(s -> System.out.println("收到: " + s));

// 时间线：
// 0ms   → 程序继续执行（不阻塞）
// 500ms → 打印 "收到: 0"
// 1000ms→ 打印 "收到: 1"
// 1500ms→ 打印 "收到: 2"
// 完成

// ★ 关键：subscribe() 后程序不会停，数据是"稍后"才到的

// ─────────────────────────────────────────────────────────────
// 例2：模拟 API 调用 - 网络延迟
Flux<String> apiFlux = Flux.create(sink -> {
    // 模拟网络请求，延迟后才有数据
    new Thread(() -> {
        try {
            Thread.sleep(300);  // 模拟网络延迟
            sink.next("数据块1");  // 300ms后到达
            
            Thread.sleep(400);
            sink.next("数据块2");  // 700ms后到达
            
            Thread.sleep(200);
            sink.next("数据块3");  // 900ms后到达
            
            sink.complete();       // 完成
        } catch (InterruptedException e) {
            sink.error(e);
        }
    }).start();
});

apiFlux.subscribe(
    data -> System.out.println("收到数据: " + data),
    error -> System.err.println("错误: " + error),
    () -> System.out.println("完成!")
);

// 时间线：
// 0ms   → subscribe() 返回，程序继续
// 300ms → 收到数据: 数据块1
// 700ms → 收到数据: 数据块2
// 900ms → 收到数据: 数据块3
// 900ms → 完成!

// ─────────────────────────────────────────────────────────────
// 例3：与阻塞代码对比

// 阻塞方式（同步）：
System.out.println("开始");
String result1 = callApi();  // 等待500ms
System.out.println(result1);
String result2 = callApi();  // 再等待500ms
System.out.println(result2);
// 用户要等 1000ms 才能看到第一个结果

// Flux方式（异步流）：
Flux<String> streamingApi = callApiStreaming();
streamingApi.subscribe(data -> System.out.println(data));
// 数据到达时立即打印，用户不用等全部完成

// 时间线对比：
// 阻塞：    [等待500ms]→结果1→[等待500ms]→结果2
// Flux:     [不等待]→...数据1到达→打印→...数据2到达→打印
```

**关键区别**：

| 特性 | List | Flux |
|-----|------|------|
| 数据获取 | 一次性全部拿到 | 逐个、可能延迟到达 |
| 执行时机 | 立即执行 | 订阅后，数据到达时执行 |
| 阻塞 | 阻塞等待 | 非阻塞，回调通知 |
| 适用场景 | 数据已在内存 | 数据来自外部（API、网络、定时器） |

**理解"异步"的关键**：
```
同步（阻塞）：
用户代码 ──调用API──→ [等待...] ──收到结果──→ 继续

异步（Flux）：
用户代码 ──调用API──→ 立即返回 ──→ 做其他事
                           │
                           ↓
                    [后台] 数据到达时触发回调
                           │
                           ↓
                    回调函数执行
```

#### Flux 的"冷"和"热"

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           冷流 (Cold Flux)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   特点：每次订阅从头开始，订阅者独享数据                                        │
│                                                                             │
│   Flux.just("A", "B", "C")                                                 │
│                                                                             │
│   订阅者1 ──→ 收到 A, B, C                                                  │
│   订阅者2 ──→ 收到 A, B, C (重新执行)                                        │
│                                                                             │
│   类比：看 Netflix 电影 - 每次点击播放从头开始                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           热流 (Hot Flux)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   特点：数据独立于订阅者，后订阅者可能错过之前数据                               │
│                                                                             │
│   Flux.interval(Duration.ofSeconds(1))  // 每秒发射一个数字                  │
│                                                                             │
│   订阅者1 (0秒加入) ──→ 收到 0, 1, 2, 3...                                   │
│   订阅者2 (2秒加入) ──→ 收到 2, 3, 4... (错过了 0, 1)                        │
│                                                                             │
│   类比：看电视直播 - 晚加入就错过之前的节目                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Flux 操作符详解

#### 常用操作符及其含义

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Flux 操作符对比                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  map() - 转换每个元素                                                        │
│  ─────────────────────                                                      │
│  Flux.just(1, 2, 3)                                                         │
│      .map(i -> i * 2)                                                       │
│  结果: 2, 4, 6                                                              │
│                                                                             │
│  filter() - 过滤元素                                                        │
│  ─────────────────────                                                      │
│  Flux.just(1, 2, 3, 4, 5)                                                   │
│      .filter(i -> i > 2)                                                    │
│  结果: 3, 4, 5                                                              │
│                                                                             │
│  flatMap() - 每个元素展开为新的 Flux                                         │
│  ─────────────────────                                                      │
│  Flux.just("A", "B")                                                        │
│      .flatMap(s -> Flux.just(s, s.toLowerCase()))                          │
│  结果: A, a, B, b                                                           │
│                                                                             │
│  handle() - 手动处理每个元素（可输出0~N个）                                   │
│  ─────────────────────                                                      │
│  Flux.just(1, 2, 3)                                                         │
│      .handle((item, sink) -> {                                              │
│          if (item == 2) {                                                   │
│              sink.next("two");                                              │
│              sink.next("二");  // 可以输出多个                               │
│          } else {                                                           │
│              sink.next(item.toString());                                    │
│          }                                                                  │
│      })                                                                     │
│  结果: "1", "two", "二", "3"                                                │
│                                                                             │
│  ★ handle() 是实现 streaming 处理的关键操作符                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### collectList() 的陷阱

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    collectList() - 把流变成批量                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  错误用法（破坏 streaming）：                                                │
│  ─────────────────────                                                      │
│                                                                             │
│  Flux<Message> apiStream = callAPI();                                       │
│                                                                             │
│  // ❌ 错误：等待所有消息完成                                                 │
│  apiStream                                                                  │
│      .collectList()  // ← 阻塞！把 Flux<List<Message>>                      │
│      .flatMapMany(messages -> processAll(messages));                       │
│                                                                             │
│  时间线：                                                                    │
│  ───────────────────────────────────────────────────────────────────────    │
│  API streaming: │ msg1 │ msg2 │ msg3 │ msg4 │ done │                       │
│                 │      │      │      │      │  ↓   │                       │
│  处理开始:      │      │      │      │      │ 开始 │ ← 全部等完才处理        │
│                                                                             │
│  ★ 这完全破坏了 streaming 的意义！                                           │
│                                                                             │
│  正确用法（保持 streaming）：                                                │
│  ─────────────────────                                                      │
│                                                                             │
│  // ✅ 正确：每个消息到达时立即处理                                           │
│  apiStream                                                                  │
│      .handle((msg, sink) -> {                                               │
│          sink.next(msg);  // 立即输出                                        │
│          // 立即处理副作用                                                   │
│          if (msg.hasToolUse()) {                                            │
│              executor.addTool(msg);  // 不等，直接执行                       │
│          }                                                                  │
│      });                                                                    │
│                                                                             │
│  时间线：                                                                    │
│  ───────────────────────────────────────────────────────────────────────    │
│  API streaming: │ msg1 │ msg2 │ msg3 │ msg4 │ done │                       │
│  处理:          │ 处理 │ 处理 │ 处理 │ 处理 │      │ ← 边收边处理            │
│  工具执行:      │      │ 开始 │ 完成 │ 开始 │      │ ← streaming期间执行     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.4 TypeScript async generator vs Java Flux

#### TypeScript 的 async generator

```typescript
// TypeScript - async generator
async function* queryLoop(): AsyncGenerator<Message> {
    // 调用 API，返回一个 async iterator
    const stream = await callModelStreaming();
    
    // for await - 每个消息到达时执行
    for await (const message of stream) {
        // 1. 立即 yield（输出）
        yield message;
        
        // 2. 如果是 tool_use，立即执行
        if (message.toolUses) {
            for (const tool of message.toolUses) {
                executor.addTool(tool);  // 不等，直接执行
            }
        }
        
        // 3. 获取已完成的结果
        for (const result of executor.getCompletedResults()) {
            yield result;  // 已经完成的工具结果
        }
    }
    
    // stream 结束后，收集剩余结果
    for (const result of await executor.getRemainingResults()) {
        yield result;
    }
}
```

#### Java Flux 的等价实现

```java
// Java - Flux 等价实现
Flux<QueryEvent> executeOneTurnStreaming(String prompt) {
    // 调用 API，返回一个 Flux
    Flux<Message> apiStream = session.sendMessageStreaming(prompt);
    
    // handle() - 每个消息到达时执行
    Flux<QueryEvent> streamingEvents = apiStream
        .handle((message, sink) -> {
            // 1. 立即输出
            sink.next(new QueryEvent.Message(message));
            
            // 2. 如果是 tool_use，立即执行
            if (message instanceof Message.Assistant) {
                for (ContentBlock.ToolUse tool : tools) {
                    executor.addTool(tool);  // 不等，直接执行
                }
            }
            
            // 3. 获取已完成的结果
            for (var result : executor.getCompletedResults()) {
                sink.next(result);  // 已经完成的工具结果
            }
        });
    
    // stream 结束后，收集剩余结果
    Flux<QueryEvent> remainingResults = Mono.fromFuture(executor.getRemainingResults())
        .flatMapMany(results -> Flux.fromIterable(results));
    
    // 组合：start → streaming → remaining
    return Flux.concat(
        Flux.just(new QueryEvent.RequestStart()),
        streamingEvents,
        remainingResults
    );
}
```

### 6.5 对应关系表

| TypeScript | Java Flux | 说明 |
|------------|-----------|------|
| `async function*` | `Flux.defer()` | 延迟创建，每次订阅执行 |
| `yield value` | `sink.next(value)` | 输出一个元素 |
| `for await (const x of stream)` | `.handle((x, sink) -> ...)` | 逐个处理 |
| `await future` | `Mono.fromFuture(future)` | 将 CompletableFuture转为Mono |
| `return` (generator结束) | `sink.complete()` | 流结束 |
| `throw error` | `sink.error(e)` | 流出错 |
| `[...results]` | `Flux.fromIterable(results)` | 从集合创建Flux |

### 6.6 完整代码流程解析

#### QueryEngine 的核心方法

```java
// 1. 入口方法 - 启动 agentic loop
public Flux<QueryEvent> executeAgenticLoop(String prompt) {
    return Flux.defer(() -> {
        // 重置状态
        interrupted = false;
        toolBlocks.clear();
        return doExecuteLoop(prompt, 0);  // 开始第一轮
    });
}

// 2. 循环控制 - 判断是否继续
private Flux<QueryEvent> doExecuteLoop(String prompt, int iteration) {
    // 检查终止条件
    if (interrupted || iteration >= maxTurns) {
        return Flux.just(new QueryEvent.Terminal(...));
    }
    
    // 执行一轮
    return executeOneTurnStreaming(prompt)
        .expand(event -> {
            // 如果有工具调用，继续下一轮
            if (event instanceof QueryEvent.ToolsComplete) {
                return doExecuteLoop(null, iteration + 1);
            }
            return Flux.empty();
        })
        .takeUntil(event -> event instanceof QueryEvent.Terminal);
}

// 3. 单轮执行 - 核心 streaming 处理
private Flux<QueryEvent> executeOneTurnStreaming(String prompt) {
    Flux<Message> apiStream = session.sendMessageStreaming(prompt);
    
    // ★ 关键：handle() 实现边收边处理
    Flux<QueryEvent> streamingEvents = apiStream
        .handle((message, sink) -> {
            // 立即输出消息
            sink.next(new QueryEvent.Message(message));
            
            // 立即处理 tool_use
            if (message.hasToolUse()) {
                executor.addTool(...);
            }
            
            // 立即获取已完成结果
            for (var result : executor.getCompletedResults()) {
                sink.next(result);
            }
        });
    
    // 收集剩余结果
    Flux<QueryEvent> remainingResults = ...;
    
    return Flux.concat(startEvent, streamingEvents, remainingResults);
}
```

#### 时间线对比

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    正确实现的时间线                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  时间 (ms)    0    100   200   300   400   500   600   700   800           │
│                                                                             │
│  API streaming    │     │     │     │     │     │     │     │ done        │
│  发送的内容:      │text │tool1│text │tool2│text │     │     │             │
│                                                                             │
│  handle()处理     │处理 │处理 │处理 │处理 │处理 │     │     │             │
│  sink.next输出    │text │tool │text │tool │text │     │     │             │
│                                                                             │
│  工具执行         │     │开始 │     │开始 │     │完成 │完成 │             │
│  executor.addTool │     │ ↓   │     │ ↓   │     │     │     │             │
│                                                                             │
│  getCompleted     │     │     │完成 │     │完成 │     │全部 │             │
│  Results输出      │     │     │result│     │result│     │results│         │
│                                                                             │
│  用户看到         │text │tool │text │result│text │result│results│done     │
│                  │     │开始 │     │1    │     │2    │全部  │             │
│                                                                             │
│  ★ 工具在 API streaming期间就开始执行                                        │
│  ★ 用户边看边等，有工具结果就立即显示                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                    错误实现的时间线                                           │
│                    (使用 collectList)                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  时间 (ms)    0    100   200   300   400   500   600   700   800           │
│                                                                             │
│  API streaming    │     │     │     │     │     │     │     │ done        │
│  发送的内容:      │text │tool1│text │tool2│text │     │     │             │
│                                                                             │
│  collectList等待  │等...│等...│等...│等...│等...│等...│等...│ 收集完      │
│                                                                             │
│  processMessages  │     │     │     │     │     │     │     │ 开始        │
│  工具执行         │     │     │     │     │     │     │     │ 开始        │
│                                                                             │
│  用户看到         │     │     │     │     │     │     │     │ 全部        │
│                  │     │     │     │     │     │     │     │ (等很久)     │
│                                                                             │
│  ★ 用户要等 API 完全结束才能看到任何内容                                      │
│  ★ 工具执行要等 API 结束才开始                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.7 线程模型详解（从线程角度理解 Flux）

#### List 遍历的线程模型

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    List 遍历 - 单线程（主线程）                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   主线程 (main)                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ for (String s : list) {                                               │
│   │      ─────────────────────────────────────────────                    │
│   │      │ System.out.println(s);  // 在主线程执行                         │
│   │      │ System.out.println(s);  // 在主线程执行                         │
│   │      │ System.out.println(s);  // 在主线程执行                         │
│   │      ─────────────────────────────────────────────                    │
│   │  }                                                                      │
│   │                                                                         │
│   └─ 继续执行后续代码...                                                     │
│                                                                             │
│   时间线：                                                                   │
│   ─────────────────────────────────────────────────────────────────────    │
│   主线程:  [打印A] → [打印B] → [打印C] → 后续代码                           │
│            ↑         ↑         ↑                                           │
│           同步      同步      同步                                          │
│                                                                             │
│   ★ 整个过程都在主线程，顺序执行，阻塞式                                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Flux 默认的线程模型（不指定 scheduler）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                Flux.just() - 默认也在主线程执行                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   主线程 (main)                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ Flux<String> flux = Flux.just("A", "B", "C");                         │
│   │  // ↑ 此时什么都不执行，只是定义                                         │
│   │                                                                         │
│   ├─ flux.subscribe(s -> System.out.println(s));                           │
│   │  // ↑ 订阅时执行，默认在当前线程（主线程）                               │
│   │  //                                                                     │
│   │  // 执行过程：                                                          │
│   │  //   打印 A（在主线程）                                                │
│   │  //   打印 B（在主线程）                                                │
│   │  //   打印 C（在主线程）                                                │
│   │                                                                         │
│   └─ 继续执行后续代码...                                                     │
│                                                                             │
│   时间线：                                                                   │
│   ─────────────────────────────────────────────────────────────────────    │
│   主线程:  [定义flux] → [订阅] → [打印A] → [打印B] → [打印C] → 后续代码     │
│                                    ↑         ↑         ↑                   │
│                                  也在主线程   也在主线程  也在主线程         │
│                                                                             │
│   ★ Flux.just() 默认也在主线程同步执行                                       │
│   ★ 看起来和 List 没区别！                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**那 Flux 的异步体现在哪里？** → 需要引入 **Scheduler（调度器）**

#### Flux 异步的关键：Scheduler

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Scheduler - 控制在哪个线程执行                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   常用 Scheduler：                                                          │
│   ─────────────────────────────────────────────────────────────────────    │
│   • Schedulers.immediate()  - 当前线程（默认）                              │
│   • Schedulers.single()     - 单独一个线程                                  │
│   • Schedulers.parallel()   - 线程池（适合 CPU 密集）                       │
│   • Schedulers.boundedElastic() - 弹性线程池（适合 IO 阻塞操作）            │
│                                                                             │
│   两个关键操作符：                                                          │
│   ─────────────────────────────────────────────────────────────────────    │
│   • subscribeOn() - 控制源头在哪个线程发射数据                              │
│   • publishOn()   - 控制下游在哪个线程处理数据                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 真正的异步 Flux 示例

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              subscribeOn(Schedulers.boundedElastic()) - 异步执行            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   代码：                                                                    │
│   ─────────────────────────────────────────────────────────────────────    │
│   Flux<String> flux = Flux.just("A", "B", "C")                              │
│       .delayElements(Duration.ofMillis(500))  // 每个元素延迟500ms          │
│       .subscribeOn(Schedulers.boundedElastic()); // 在弹性线程池执行        │
│                                                                             │
│   flux.subscribe(s -> System.out.println(                                   │
│       Thread.currentThread().getName() + ": " + s                          │
│   ));                                                                       │
│                                                                             │
│   System.out.println("主线程继续: " + Thread.currentThread().getName());    │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   线程协作图：                                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   主线程 (main)                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ [定义 flux]                                                            │
│   ├─ [subscribe() 订阅]                                                     │
│   │     │                                                                   │
│   │     └──→ 提交任务到 boundedElastic 线程池                              │
│   │          │                                                              │
│   │          ↓                                                              │
│   │   ─────────────────────────────────────                                │
│   │   │ 立即返回！                          │                                │
│   │   ─────────────────────────────────────                                │
│   │                                                                         │
│   ├─ [打印: 主线程继续: main]  ← 不等待，直接执行                           │
│   │                                                                         │
│   └─ 主线程结束（或做其他事）                                                │
│                                                                             │
│                                                                             │
│   boundedElastic 线程池 (某个工作线程)                                       │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   │   [等待 500ms]                                                          │
│   │   [打印: boundedElastic-1: A]                                          │
│   │   [等待 500ms]                                                          │
│   │   [打印: boundedElastic-1: B]                                          │
│   │   [等待 500ms]                                                          │
│   │   [打印: boundedElastic-1: C]                                          │
│   │   [完成]                                                                │
│   │                                                                         │
│   └────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   输出顺序：                                                                │
│   ─────────────────────────────────────────────────────────────────────    │
│   1. 主线程继续: main              ← 主线程先打印                          │
│   2. boundedElastic-1: A           ← 500ms 后                              │
│   3. boundedElastic-1: B           ← 1000ms 后                             │
│   4. boundedElastic-1: C           ← 1500ms 后                             │
│                                                                             │
│   ★ 主线程不被阻塞，数据在另一个线程慢慢产生                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### QueryEngine 实际的线程模型

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    QueryEngine 的线程协作                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   代码流程：                                                                │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // Main.java                                                              │
│   Flux<QueryEvent> eventFlux = queryEngine.executeAgenticLoop(prompt);     │
│                                                                             │
│   CountDownLatch latch = new CountDownLatch(1);                            │
│   eventFlux.subscribe(                                                      │
│       event -> print(event),      // onNext 回调                           │
│       error -> latch.countDown(), // onError 回调                          │
│       () -> latch.countDown()      // onComplete 回调                      │
│   );                                                                        │
│                                                                             │
│   latch.await(300, TimeUnit.SECONDS);  // 主线程等待完成                    │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   线程协作图：                                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   主线程 (main)                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ [executeAgenticLoop() 定义 Flux]                                      │
│   │     │                                                                   │
│   │     └── 返回 Flux（此时什么都不执行）                                   │
│   │                                                                         │
│   ├─ [subscribe() 订阅]                                                     │
│   │     │                                                                   │
│   │     └── 触发 Flux 执行，但立即返回                                      │
│   │         │                                                               │
│   │         └──→ 提交到 Reactor 默认线程池                                  │
│   │                                                                         │
│   ├─ [latch.await() 等待]                                                   │
│   │     │                                                                   │
│   │     └── 主线程阻塞在这里，等待 latch.countDown()                        │
│   │                                                                         │
│                                                                             │
│                                                                             │
│   Reactor 线程池 (parallel-1 或类似)                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ [API 请求] ──→ HTTP 客户端线程池                                       │
│   │                   │                                                     │
│   │                   └── [等待 API 响应]                                   │
│   │                         │                                               │
│   │                         └── 响应回来后，触发 handle() 回调              │
│   │                               │                                         │
│   │                               ├─ [sink.next(message)]                  │
│   │                               │     │                                   │
│   │                               │     └──→ 触发主线程的 onNext 回调       │
│   │                               │         (print(event))                  │
│   │                               │                                         │
│   │                               └─ [executor.addTool()]                   │
│   │                                     │                                   │
│   │                                     └──→ 提交到工具执行线程池           │
│   │                                                                         │
│   └─ [streaming 结束]                                                       │
│         │                                                                   │
│         └── [getRemainingResults() 等待工具完成]                            │
│               │                                                             │
│               └── [latch.countDown()] ──→ 唤醒主线程                       │
│                                                                             │
│                                                                             │
│   工具执行线程池 (ForkJoinPool 或 CompletableFuture 默认池)                  │
│   ─────────────────────────────────────────────────────────────────────    │
│   │                                                                         │
│   ├─ [Tool1 执行] (可能在 pool-1)                                          │
│   │     │                                                                   │
│   │     └── 完成后通知 StreamingToolExecutor                               │
│   │                                                                         │
│   ├─ [Tool2 执行] (可能在 pool-2，并发)                                     │
│   │     │                                                                   │
│   │     └── 完成后通知 StreamingToolExecutor                               │
│   │                                                                         │
│   └─ 工具结果被 getCompletedResults() 或 getRemainingResults() 收集        │
│                                                                             │
│                                                                             │
│   完整时间线：                                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   时间   主线程              Reactor线程          工具线程                  │
│   ───   ────────             ───────────          ───────                  │
│   0ms   subscribe()          │                    │                         │
│         │                    │                    │                         │
│   1ms   await(阻塞)          API请求              │                         │
│         │                    │                    │                         │
│   100ms │                    收到text             │                         │
│         │←───────────────────│                    │                         │
│         │ print(text)        │                    │                         │
│         │                    │                    │                         │
│   200ms │                    收到tool_use         │                         │
│         │                    │                    │                         │
│         │                    addTool() ──────────→│ Tool1开始执行           │
│         │                    │                    │                         │
│   300ms │                    收到text             │                         │
│         │←───────────────────│                    │                         │
│         │ print(text)        │                    Tool1完成                │
│         │                    │←───────────────────│                         │
│         │                    收集结果             │                         │
│         │←───────────────────│                    │                         │
│         │ print(result1)     │                    │                         │
│         │                    │                    │                         │
│   ...   │                    ...                  ...                       │
│         │                    │                    │                         │
│   done  │                    完成                 │                         │
│         │←───────────────────│                    │                         │
│         │ latch.countDown()  │                    │                         │
│         │                    │                    │                         │
│         ↓ (主线程继续)       │                    │                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 线程协作的关键点

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         线程协作总结                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. 主线程（调用线程）                                                      │
│      ─────────────────────────────────────────────────────────────────    │
│      • 调用 subscribe() 触发执行                                            │
│      • await() 等待完成（CLI 场景需要阻塞）                                 │
│      • 收到 onNext 回调时打印输出                                           │
│                                                                             │
│   2. Reactor 线程池                                                         │
│      ─────────────────────────────────────────────────────────────────    │
│      • 执行 Flux 操作符链                                                   │
│      • 处理 API streaming 响应                                              │
│      • 调用 handle() 中的回调                                               │
│      • 通过 sink.next() 触发主线程的 onNext                                 │
│                                                                             │
│   3. HTTP 客户端线程池                                                      │
│      ─────────────────────────────────────────────────────────────────    │
│      • 执行网络 I/O                                                         │
│      • 等待 API 响应                                                        │
│      • 响应到达后通知 Reactor 线程                                          │
│                                                                             │
│   4. 工具执行线程池                                                         │
│      ─────────────────────────────────────────────────────────────────    │
│      • StreamingToolExecutor.addTool() 提交任务                             │
│      • CompletableFuture.runAsync() 在线程池执行                           │
│      • 工具完成后通知 StreamingToolExecutor                                 │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│   通信方式：                                                                │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   • Flux/Sink 机制：sink.next() → 触发订阅者的 onNext 回调                 │
│   • CompletableFuture：工具异步执行，完成后回调                             │
│   • CountDownLatch：主线程等待全部完成                                      │
│   • CopyOnWriteArrayList：线程安全的共享状态                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 为什么比同步 List 快？

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         同步 vs 异步对比                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   同步 List 模式（假设每个工具执行 500ms）：                                 │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   主线程:  [API调用 300ms] → [Tool1 500ms] → [Tool2 500ms] → 完成          │
│                                                                             │
│   总时间: 300 + 500 + 500 = 1300ms                                          │
│                                                                             │
│                                                                             │
│   异步 Flux 模式：                                                          │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   主线程:    [subscribe] → [await] ──────────────────────→ 完成            │
│                                │                           ↑               │
│   Reactor:  [API 300ms] ──────┤                           │               │
│                            收到tool1, tool2                │               │
│                                │                           │               │
│   Tool线程: [Tool1 500ms] ─────┼───────────────────────────┤               │
│              [Tool2 500ms] ─────┼───────────────────────────┘               │
│                                │                                           │
│                                └── 并发执行！                               │
│                                                                             │
│   总时间: 300 + 500 = 800ms  (Tool1 和 Tool2 并发)                         │
│                                                                             │
│   ★ 节省时间：API streaming 期间工具就开始执行                               │
│   ★ 多工具并发：Tool1 和 Tool2 同时执行                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.8 Flux.defer 源码原理

#### 核心接口：Publisher.subscribe()

```java
// Reactive Streams 核心接口
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> s);
}
```

**关键点**：`subscribe()` 是整个响应式流的触发入口，定义时不会执行，只有调用 `subscribe()` 时才开始。

---

#### Flux.just 的源码实现

```java
// Flux.just("A", "B", "C") 实际创建的是 FluxArray
public static <T> Flux<T> just(T... data) {
    return onAssembly(new FluxArray(data));
}

// FluxArray 源码（简化）
final class FluxArray<T> extends Flux<T> {
    final T[] array;  // ★ 数据在构造时就存好了

    FluxArray(T[] array) {
        this.array = array;  // 定义时就确定
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        // ★ 订阅时，数据已经存在，直接发射
        actual.onSubscribe(new ArraySubscription(actual, array));
    }
}

// ArraySubscription 发射数据
class ArraySubscription<T> {
    final T[] array;
    int index = 0;

    void fastPath() {
        for (int i = 0; i < array.length; i++) {
            actual.onNext(array[i]);  // 遍历数组，逐个发射
        }
        actual.onComplete();
    }
}
```

**流程**：
```
定义阶段：Flux.just("A", "B", "C")
    │
    └──→ new FluxArray(["A", "B", "C"])  // 数据已确定

订阅阶段：flux.subscribe(subscriber)
    │
    └──→ subscribe() 被调用
         │
         └──→ ArraySubscription.fastPath()
              │
              └──→ onNext("A") → onNext("B") → onNext("C") → onComplete()
```

---

#### Flux.defer 的源码实现

```java
// Flux.defer() 创建的是 FluxDefer
public static <T> Flux<T> defer(Supplier<? extends Publisher<T>> supplier) {
    return onAssembly(new FluxDefer(supplier));
}

// FluxDefer 源码
final class FluxDefer<T> extends Flux<T> {
    final Supplier<? extends Publisher<? extends T>> supplier;  // ★ 只存了一个 Supplier

    FluxDefer(Supplier<? extends Publisher<? extends T>> supplier) {
        this.supplier = supplier;  // 只存引用，不执行
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        Publisher<? extends T> p;
        try {
            // ★★★ 关键：订阅时才调用 supplier.get()
            p = supplier.get();
        } catch (Throwable e) {
            Operators.error(actual, e);
            return;
        }
        // 然后向新创建的 Publisher 订阅
        Flux.from(p).subscribe(actual);
    }
}
```

**流程**：
```
定义阶段：Flux.defer(() -> Flux.just(time.now()))
    │
    └──→ new FluxDefer(supplier)  // 只存了 supplier，没执行

订阅阶段：flux.subscribe(subscriber)
    │
    └──→ FluxDefer.subscribe() 被调用
         │
         ├──→ supplier.get()  // ★★★ 现在才执行！
         │    │
         │    └──→ 返回新的 Flux.just(time.now())
         │
         └──→ 向新的 Flux 订阅
              │
              └──→ onNext(...) → onComplete()
```

---

#### 对比图解

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Flux.just vs Flux.defer 源码对比                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Flux.just("A", "B", "C")                                                 │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌─────────────────┐     ┌─────────────────────────────────────────────┐  │
│   │   定义阶段       │     │   new FluxArray(["A", "B", "C"])            │  │
│   │                 │ ──→ │   this.array = ["A", "B", "C"]  ← 数据固定   │  │
│   │                 │     │                                             │  │
│   └─────────────────┘     └─────────────────────────────────────────────┘  │
│                                                                             │
│   ┌─────────────────┐     ┌─────────────────────────────────────────────┐  │
│   │   订阅阶段       │     │   subscribe(actual)                         │  │
│   │                 │ ──→ │     └──→ 直接用已存在的 array 发射           │  │
│   │                 │     │           onNext("A") → onNext("B") → ...   │  │
│   └─────────────────┘     └─────────────────────────────────────────────┘  │
│                                                                             │
│   ★ 数据在定义时就确定了                                                    │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   Flux.defer(() -> Flux.just(time.now()))                                  │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌─────────────────┐     ┌─────────────────────────────────────────────┐  │
│   │   定义阶段       │     │   new FluxDefer(supplier)                   │  │
│   │                 │ ──→ │   this.supplier = supplier  ← 只存引用      │  │
│   │                 │     │   supplier.get() 没有被调用！                │  │
│   └─────────────────┘     └─────────────────────────────────────────────┘  │
│                                                                             │
│   ┌─────────────────┐     ┌─────────────────────────────────────────────┐  │
│   │   订阅阶段       │     │   subscribe(actual)                         │  │
│   │                 │     │     │                                        │  │
│   │                 │ ──→ │     ├──→ p = supplier.get()  ← 现在才执行！  │  │
│   │                 │     │     │       │                                │  │
│   │                 │     │     │       └──→ 创建新的 Flux.just(...)     │  │
│   │                 │     │     │                                        │  │
│   │                 │     │     └──→ p.subscribe(actual)                 │  │
│   │                 │     │           onNext(...) → onComplete()         │  │
│   └─────────────────┘     └─────────────────────────────────────────────┘  │
│                                                                             │
│   ★ 数据在订阅时才创建，每次订阅都是新的                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### 多次订阅的行为差异

```java
// Flux.just - 多次订阅，数据相同
Flux<String> flux1 = Flux.just(getData());  // getData() 只调用一次

flux1.subscribe(s -> print(s));  // 输出相同数据
flux1.subscribe(s -> print(s));  // 输出相同数据

// 源码原因：array 在构造时就固定了


// Flux.defer - 多次订阅，数据可能不同
Flux<String> flux2 = Flux.defer(() -> Flux.just(getData()));  // getData() 没调用

flux2.subscribe(s -> print(s));  // getData() 调用第1次
flux2.subscribe(s -> print(s));  // getData() 调用第2次

// 源码原因：每次 subscribe() 都执行 supplier.get()
```

---

#### QueryEngine 中的应用

```java
public Flux<QueryEvent> executeAgenticLoop(String prompt) {
    return Flux.defer(() -> {
        // ★ 这些代码在 subscribe() 时才执行
        interrupted = false;
        toolBlocks.clear();
        return doExecuteLoop(prompt, 0);
    });
}

// 第一次订阅
flux.subscribe(...);
// → 执行 supplier.get()
// → interrupted = false, toolBlocks.clear()
// → 创建新的 Flux 执行

// 第二次订阅
flux.subscribe(...);
// → 再次执行 supplier.get()
// → interrupted = false, toolBlocks.clear()  ← 状态重置！
// → 创建新的 Flux 执行
```

---

#### 核心原理总结

| 操作 | 定义时 | 订阅时 |
|-----|-------|-------|
| `Flux.just(data)` | 数据确定 | 直接发射已确定的数据 |
| `Flux.defer(supplier)` | 只存 supplier 引用 | 调用 `supplier.get()`，创建新的 Publisher |

**defer 的本质**：把"创建 Publisher"这个动作延迟到订阅时才执行。

---

### 6.9 订阅传播机制详解

#### Reactive Streams 核心接口

```java
// 4个核心接口
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> s);  // 被订阅
}

public interface Subscriber<T> {
    void onSubscribe(Subscription s);  // 收到订阅确认
    void onNext(T t);                   // 收到数据
    void onError(Throwable t);          // 收到错误
    void onComplete();                  // 收到完成信号
}

public interface Subscription {
    void request(long n);  // 请求 n 个数据（背压）
    void cancel();         // 取消订阅
}
```

---

#### 订阅传播流程（逆向）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    订阅传播源码流程                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   代码：                                                                    │
│   flux.map(f).filter(p).subscribe(handler);                                │
│                                                                             │
│   源码执行过程：                                                            │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   步骤1：subscribe(handler) 被调用                                          │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // 在 FluxFilter 中（filter 返回的是 FluxFilter）                         │
│   public void subscribe(CoreSubscriber<? super T> actual) {                │
│       // actual = LambdaSubscriber(handler)                                │
│       // source = FluxMap (上游)                                            │
│       source.subscribe(new FluxFilter.FilterSubscriber(actual, predicate));│
│       //                 ↑ 创建新的 Subscriber 包装                         │
│   }                                                                         │
│                                                                             │
│   歌骤2：FluxMap.subscribe() 被调用                                         │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // 在 FluxMap 中                                                          │
│   public void subscribe(CoreSubscriber<? super T> actual) {                │
│       // actual = FilterSubscriber                                          │
│       // source = FluxArray (上游)                                          │
│       source.subscribe(new FluxMap.MapSubscriber(actual, mapper));         │
│       //                 ↑ 创建新的 Subscriber 包装                         │
│   }                                                                         │
│                                                                             │
│   步骤3：FluxArray.subscribe() 被调用                                       │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // 在 FluxArray 中（源头）                                                 │
│   public void subscribe(CoreSubscriber<? super T> actual) {                │
│       // actual = MapSubscriber                                             │
│       actual.onSubscribe(new ArraySubscription(actual, array));            │
│       //     ↑ 调用下游的 onSubscribe，传入 Subscription                   │
│   }                                                                         │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   订阅链建立完成：                                                          │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   LambdaSubscriber ← FilterSubscriber ← MapSubscriber ← ArraySubscription │
│       ↓                    ↓                  ↓               ↓           │
│    处理最终            过滤处理           map转换           数据源          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### 数据流传播流程（顺向）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    数据流传播源码流程                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ArraySubscription.fastPath() 开始发射数据                                 │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   void fastPath() {                                                         │
│       for (int i = 0; i < array.length; i++) {                             │
│           T t = array[i];                                                   │
│           actual.onNext(t);  // actual = MapSubscriber                     │
│       }                                                                     │
│       actual.onComplete();                                                  │
│   }                                                                         │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   MapSubscriber.onNext() 处理并传递                                         │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   public void onNext(T t) {                                                │
│       U result = mapper.apply(t);  // 执行 map 函数                         │
│       actual.onNext(result);  // actual = FilterSubscriber                 │
│   }                                                                         │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   FilterSubscriber.onNext() 过滤并传递                                      │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   public void onNext(T t) {                                                │
│       if (predicate.test(t)) {  // 测试是否通过                             │
│           actual.onNext(t);  // actual = LambdaSubscriber                  │
│       }                                                                     │
│   }                                                                         │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   LambdaSubscriber.onNext() 最终处理                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   public void onNext(T t) {                                                │
│       handler.accept(t);  // 用户定义的 handler                             │
│   }                                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### 完整时间线

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    完整执行时间线                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   时间  源码调用                              数据状态                       │
│   ───  ────────────────────────────────────  ──────────                    │
│                                                                             │
│   阶段1：订阅传播（逆向）                                                    │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   T0   .subscribe(handler)                   -                             │
│        → 创建 LambdaSubscriber                                             │
│                                                                             │
│   T1   FluxFilter.subscribe(LambdaSubscriber)                             │
│        → 创建 FilterSubscriber                                             │
│                                                                             │
│   T2   FluxMap.subscribe(FilterSubscriber)                                 │
│        → 创建 MapSubscriber                                                │
│                                                                             │
│   T3   FluxArray.subscribe(MapSubscriber)                                  │
│        → 创建 ArraySubscription                                            │
│        → 调用 MapSubscriber.onSubscribe(subscription)                      │
│                                                                             │
│   T4   MapSubscriber.onSubscribe()                                         │
│        → 调用 FilterSubscriber.onSubscribe(subscription)                   │
│                                                                             │
│   T5   FilterSubscriber.onSubscribe()                                      │
│        → 调用 LambdaSubscriber.onSubscribe(subscription)                   │
│                                                                             │
│   T6   LambdaSubscriber.onSubscribe()                                      │
│        → subscription.request(Long.MAX_VALUE)  请求所有数据                │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   阶段2：数据流（顺向）                                                      │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   T7   ArraySubscription.request(n)                                        │
│        → 开始 fastPath()                                                   │
│                                                                             │
│   T8   emit "A":                                                            │
│        → ArraySubscription.onNext("A")                                     │
│        → MapSubscriber.onNext("A") → map("A") → "A!"                       │
│        → FilterSubscriber.onNext("A!") → test("A!") → true                 │
│        → LambdaSubscriber.onNext("A!") → handler("A!")                     │
│                                                                             │
│   T9   emit "B":                                                            │
│        → 同上...                                                           │
│                                                                             │
│   T10  emit "C":                                                            │
│        → 同上...                                                           │
│                                                                             │
│   T11  ArraySubscription.onComplete()                                      │
│        → MapSubscriber.onComplete()                                        │
│        → FilterSubscriber.onComplete()                                     │
│        → LambdaSubscriber.onComplete()                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### 核心原理总结

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         核心原理                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   1. Publisher.subscribe() 触发整个流程                                     │
│      ─────────────────────────────────────────────────────────────────    │
│      • 每个操作符（map, filter 等）在 subscribe() 时创建新的 Subscriber     │
│      • 订阅从下游向上游传播，建立完整的 Subscriber 链                        │
│                                                                             │
│   2. Subscriber.onSubscribe() 确认订阅                                      │
│      ─────────────────────────────────────────────────────────────────    │
│      • 源头创建 Subscription，传给下游                                      │
│      • 下游可以通过 Subscription.request() 请求数据                        │
│                                                                             │
│   3. Subscription.request() 触发数据流                                      │
│      ─────────────────────────────────────────────────────────────────    │
│      • 源头开始发射数据                                                     │
│      • 数据通过 onNext() 从上游向下游传播                                   │
│                                                                             │
│   4. Subscriber.onNext() 处理数据                                           │
│      ─────────────────────────────────────────────────────────────────    │
│      • 每个操作符的 Subscriber 处理数据后传给下游                            │
│      • 最终到达用户定义的 handler                                           │
│                                                                             │
│   5. Subscriber.onComplete() 结束流                                         │
│      ─────────────────────────────────────────────────────────────────    │
│      • 源头发射完所有数据后调用                                             │
│      • 从上游向下游传播，通知所有 Subscriber                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### Reactor 的订阅机制：逆向传播，顺向数据流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Reactor 订阅机制核心原理                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   代码：                                                                    │
│   ─────────────────────────────────────────────────────────────────────    │
│   Flux.just("A", "B", "C")                                                  │
│       .map(s -> s + "!")                                                    │
│       .filter(s -> s.length() > 1)                                          │
│       .subscribe(s -> System.out.println(s));                               │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   阶段1：订阅阶段（逆向传播）                                                │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │   subscribe()                                                       │  │
│   │       │                                                             │  │
│   │       │ ① 创建 Subscriber，向上游发起订阅                            │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │   filter    │ ← ② filter 收到订阅，创建自己的 Subscriber        │  │
│   │   └─────────────┘      向上游订阅                                    │  │
│   │       │                                                             │  │
│   │       │ ③ 向上游发起订阅                                             │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │    map      │ ← ④ map 收到订阅，创建自己的 Subscriber           │  │
│   │   └─────────────┘      向上游订阅                                    │  │
│   │       │                                                             │  │
│   │       │ ⑤ 向上游发起订阅                                             │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │ Flux.just   │ ← ⑥ 源头收到订阅，准备发射数据                    │  │
│   │   └─────────────┘                                                   │  │
│   │                                                                     │  │
│   │   ★ 订阅阶段从下往上传播，建立完整的 Subscriber 链                    │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   阶段2：数据流阶段（顺向传播）                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                                                                     │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │ Flux.just   │ ──→ ① 发射 "A"                                   │  │
│   │   └─────────────┘      调用下游 Subscriber.onNext("A")              │  │
│   │       │                                                             │  │
│   │       │ onNext("A")                                                 │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │    map      │ ← ② 收到 "A"，转换为 "A!"                         │  │
│   │   └─────────────┘      调用下游 Subscriber.onNext("A!")             │  │
│   │       │                                                             │  │
│   │       │ onNext("A!")                                                │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │   filter    │ ← ③ 收到 "A!"，长度 > 1，通过                    │  │
│   │   └─────────────┘      调用下游 Subscriber.onNext("A!")             │  │
│   │       │                                                             │  │
│   │       │ onNext("A!")                                                │  │
│   │       │                                                             │  │
│   │       ▼                                                             │  │
│   │   ┌─────────────┐                                                   │  │
│   │   │  subscribe  │ ← ④ 最终订阅者收到，打印 "A!"                     │  │
│   │   └─────────────┘                                                   │  │
│   │                                                                     │  │
│   │   ★ 数据流从上往下传播，经过每个操作符处理                             │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   总结：                                                                    │
│   ─────────────────────────────────────────────────────────────────────    │
│   订阅阶段：下游 → 上游（建立 Subscriber 链）                                │
│   数据阶段：上游 → 下游（onNext/onError/onComplete）                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Flux.defer 的作用

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Flux.defer - 延迟创建                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   问题：Flux.just() 在定义时就确定了值                                       │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // 例1：时间在定义时就固定了                                               │
│   Flux<Long> flux1 = Flux.just(System.currentTimeMillis());                 │
│                                                                             │
│   Thread.sleep(1000);                                                       │
│                                                                             │
│   flux1.subscribe(t -> System.out.println(t));  // 打印 1秒前的时间        │
│   flux1.subscribe(t -> System.out.println(t));  // 打印 1秒前的时间(相同)  │
│                                                                             │
│   ★ 定义时就已经确定了值，每次订阅得到相同的值                               │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   解决：Flux.defer() 在订阅时才执行创建逻辑                                  │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   // 例2：使用 defer，每次订阅时重新获取时间                                 │
│   Flux<Long> flux2 = Flux.defer(() ->                                       │
│       Flux.just(System.currentTimeMillis())                                 │
│   );                                                                        │
│                                                                             │
│   Thread.sleep(1000);                                                       │
│                                                                             │
│   flux2.subscribe(t -> System.out.println(t));  // 打印当前时间            │
│   flux2.subscribe(t -> System.out.println(t));  // 打印新的当前时间        │
│                                                                             │
│   ★ 每次订阅时都重新执行 lambda，获取新的值                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### defer 的内部原理

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Flux.defer 订阅过程详解                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Flux<QueryEvent> flux = Flux.defer(() -> {                                │
│       interrupted = false;           // 重置状态                           │
│       toolBlocks.clear();            // 重置状态                           │
│       return doExecuteLoop(prompt, 0);  // 创建新的 Flux                    │
│   });                                                                       │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   第一次订阅：                                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   flux.subscribe(subscriber1);                                              │
│       │                                                                     │
│       │ ① subscribe 触发                                                   │
│       │                                                                     │
│       ▼                                                                     │
│   Flux.defer 收到订阅                                                       │
│       │                                                                     │
│       │ ② 执行 lambda: () -> { ... }                                       │
│       │    - interrupted = false                                           │
│       │    - toolBlocks.clear()                                            │
│       │    - return doExecuteLoop() → 创建新的 FluxInner                   │
│       │                                                                     │
│       ▼                                                                     │
│   向 FluxInner 发起订阅                                                     │
│       │                                                                     │
│       │ ③ FluxInner 开始执行                                               │
│       │                                                                     │
│       ▼                                                                     │
│   数据流开始：onNext → onNext → ... → onComplete                           │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   第二次订阅：                                                              │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   flux.subscribe(subscriber2);                                              │
│       │                                                                     │
│       │ ① 新的 subscribe 触发                                              │
│       │                                                                     │
│       ▼                                                                     │
│   Flux.defer 收到订阅                                                       │
│       │                                                                     │
│       │ ② 重新执行 lambda: () -> { ... }   ← ★ 再次执行！                  │
│       │    - interrupted = false   ← 重置                                  │
│       │    - toolBlocks.clear()    ← 重置                                  │
│       │    - return doExecuteLoop() → 创建全新的 FluxInner                 │
│       │                                                                     │
│       ▼                                                                     │
│   向新的 FluxInner 发起订阅                                                 │
│       │                                                                     │
│       │ ③ 全新的执行流程                                                   │
│       │                                                                     │
│       ▼                                                                     │
│   新的数据流：onNext → onNext → ... → onComplete                           │
│                                                                             │
│   ★ 每次订阅都会重新执行 lambda，创建新的 Flux 实例                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 对比：有无 defer 的区别

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         有无 defer 对比                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   无 defer（错误）：                                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   public Flux<QueryEvent> executeAgenticLoop(String prompt) {              │
│       // 这些在方法调用时就执行了，而不是订阅时                              │
│       interrupted = false;                                                  │
│       toolBlocks.clear();                                                   │
│       return doExecuteLoop(prompt, 0);                                      │
│   }                                                                         │
│                                                                             │
│   // 调用                                                                   │
│   Flux<QueryEvent> flux = engine.executeAgenticLoop("hello");              │
│   // ↑ 此时 interrupted=false, toolBlocks 已清空                           │
│   // ↑ 但还没订阅，数据还没开始流动                                         │
│                                                                             │
│   flux.subscribe(...);  // 订阅，开始数据流                                 │
│   flux.subscribe(...);  // 再次订阅，但状态已被第一次修改                    │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   有 defer（正确）：                                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   public Flux<QueryEvent> executeAgenticLoop(String prompt) {              │
│       return Flux.defer(() -> {                                             │
│           // 这些在订阅时才执行                                             │
│           interrupted = false;                                              │
│           toolBlocks.clear();                                               │
│           return doExecuteLoop(prompt, 0);                                  │
│       });                                                                   │
│   }                                                                         │
│                                                                             │
│   // 调用                                                                   │
│   Flux<QueryEvent> flux = engine.executeAgenticLoop("hello");              │
│   // ↑ 此时什么都没执行，只是定义了一个"配方"                               │
│                                                                             │
│   flux.subscribe(...);  // 订阅 → 执行 lambda → 重置状态 → 开始数据流      │
│   flux.subscribe(...);  // 再次订阅 → 再次执行 lambda → 重置状态 → 新流程  │
│                                                                             │
│   ★ 每次订阅都是全新的开始，状态被正确重置                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 完整订阅流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    QueryEngine 完整订阅流程                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   代码：                                                                    │
│   ─────────────────────────────────────────────────────────────────────    │
│   Flux<QueryEvent> flux = queryEngine.executeAgenticLoop(prompt);          │
│   flux.subscribe(                                                           │
│       event -> print(event),                                                │
│       error -> handleError(error),                                          │
│       () -> System.out.println("Done")                                      │
│   );                                                                        │
│                                                                             │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   订阅阶段（逆向）：                                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   subscribe(event -> ...)                                                   │
│           │                                                                 │
│           │ 创建 LambdaSubscriber                                           │
│           │                                                                 │
│           ▼                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ Flux.defer                                                          │  │
│   │                                                                     │  │
│   │  收到订阅后：                                                        │  │
│   │  1. 执行 supplier: () -> { interrupted=false; ... }                 │  │
│   │  2. 获得内部 Flux: doExecuteLoop(prompt, 0)                         │  │
│   │  3. 向内部 Flux 发起订阅                                             │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│           │                                                                 │
│           ▼                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ doExecuteLoop (expand + takeUntil)                                  │  │
│   │                                                                     │  │
│   │  向下游传播订阅...                                                   │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│           │                                                                 │
│           ▼                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ executeOneTurnStreaming                                             │  │
│   │                                                                     │  │
│   │  向下游传播订阅...                                                   │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│           │                                                                 │
│           ▼                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │ session.sendMessageStreaming                                        │  │
│   │                                                                     │  │
│   │  到达源头，开始发射数据                                              │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│   数据阶段（顺向）：                                                        │
│   ─────────────────────────────────────────────────────────────────────    │
│                                                                             │
│   session.sendMessageStreaming                                             │
│           │                                                                 │
│           │ 发射 Message                                                   │
│           ▼                                                                 │
│   executeOneTurnStreaming.handle()                                         │
│           │                                                                 │
│           │ 处理、可能调用 executor.addTool()                              │
│           │ sink.next(QueryEvent.Message)                                  │
│           ▼                                                                 │
│   doExecuteLoop (expand/takeUntil 处理)                                    │
│           │                                                                 │
│           │ 传递 QueryEvent                                                │
│           ▼                                                                 │
│   Flux.defer                                                               │
│           │                                                                 │
│           │ 传递 QueryEvent                                                │
│           ▼                                                                 │
│   LambdaSubscriber                                                         │
│           │                                                                 │
│           │ event -> print(event)                                          │
│           ▼                                                                 │
│   输出到控制台                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 类比理解

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         生活类比                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Flux.just("A", "B", "C")                                                 │
│       ─────────────────────────────────────────────────────────────────    │
│       类比：买了一张 DVD，内容已经固定                                       │
│       每次播放都是相同的内容                                                │
│                                                                             │
│   Flux.defer(() -> Flux.just(time.now()))                                  │
│       ─────────────────────────────────────────────────────────────────    │
│       类比：买了一张"现场直播"门票                                          │
│       每次观看都是当时的内容（不同时间看，内容不同）                         │
│                                                                             │
│   订阅阶段（逆向）：                                                        │
│       ─────────────────────────────────────────────────────────────────    │
│       类比：观众找座位 → 告诉工作人员 → 工作人员准备设备                     │
│       从观众开始，逆向传播到源头                                             │
│                                                                             │
│   数据阶段（顺向）：                                                        │
│       ─────────────────────────────────────────────────────────────────    │
│       类比：演出开始 → 观众看到表演                                         │
│       从舞台开始，顺向传播到观众                                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 更新日志

### 2026-04-06
- 添加 Reactor Flux 响应式编程文档
- 添加 Sealed Interface 文档
- 添加 Record 类型文档