# OpenAI 代码评审 SDK 接入指南

本 SDK 基于大模型（如阿里云通义千问）对 Git 提交的代码变更进行自动评审，并生成结构化报告，**帮助团队减少人工 Code Review 成本**，在提交流程中尽早发现潜在问题。

---

## 一、功能与价值

- **自动拉取代码变更**：基于 JGit 获取最近一次提交的 diff
- **AI 智能评审**：调用兼容 OpenAI 的 API（默认支持阿里云 DashScope/Qwen）进行多维度评审
- **结构化报告**：生成 Markdown 评审报告，可落盘到本地或上传到指定 GitHub 仓库
- **可选通知**：支持评审完成后通过微信公众号推送结果（可选）

**适用场景**：作为 MR/PR 的辅助评审、CI 流水线中的自动检查、或定时/事件触发的代码质量扫描。

---

## 二、接入方式概览

| 方式 | 适用场景 | 说明 |
|------|----------|------|
| **Maven 依赖 + 编程调用** | 已有 Java 项目，希望在业务/流水线中调用 | 引入依赖，使用 `CodeReviewClient` 执行评审 |
| **JAR 命令行** | 无 Java 项目，或 CI 中直接跑 JAR | 下载 fat JAR，通过 `java -jar` + 环境变量运行 |
| **GitHub Actions** | 仓库在 GitHub，希望 Push/PR 时自动评审 | 使用现成 workflow 或参考示例配置 |

以下分别说明三种接入方式。

---

## 三、方式一：Maven 依赖 + 编程式调用

### 3.1 添加依赖

若 SDK 已发布到 Maven 仓库，在项目的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>org.ocr.com</groupId>
    <artifactId>openai-code-review-sdk</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

若仅本地有构建产物，可先在本仓库执行 `mvn clean install`，再在业务项目中通过上述坐标引用；或使用 `system` 作用域 + 本地路径（不推荐长期使用）。

### 3.2 最小示例（环境变量配置）

保证运行时机器的环境变量已设置（见下方「配置说明」），至少需要 **API Key**：

```java
import org.ocr.com.sdk.api.CodeReviewClient;
import org.ocr.com.sdk.domain.model.ReviewResult;

public class MyCodeReviewRunner {
    public static void main(String[] args) {
        // 从环境变量读取配置（OPENAI_API_KEY 等）
        CodeReviewClient client = CodeReviewClient.create();
        ReviewResult result = client.review();
        System.out.println("报告路径: " + result.getReportPath());
        System.out.println("评审内容: " + result.getReviewContent());
    }
}
```

### 3.3 使用 Builder 显式配置（推荐）

不依赖环境变量，适合在流水线或配置中心中传入参数：

```java
CodeReviewClient client = CodeReviewClient.builder()
    .apiKey("your-api-key")
    .apiUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
    .model("qwen-flash")
    .gitRepositoryPath("/path/to/your/git/repo")  // 可选，默认当前目录
    .reportBaseDir("代码评审记录")
    .build();

ReviewResult result = client.review();
```

### 3.4 使用配置文件

将 `code-review.properties` 放到 classpath 根目录（或指定路径），然后：

```java
// 默认 classpath:code-review.properties
CodeReviewClient client = CodeReviewClient.createFromProperties();

// 或指定文件路径
CodeReviewClient client = CodeReviewClient.createFromProperties("/conf/code-review.properties");

ReviewResult result = client.review();
```

---

## 四、方式二：JAR 命令行（CI/CD）

适合在 Jenkins、GitLab CI、自建 CI 等环境中直接执行，无需编写 Java 代码。

### 4.1 获取 JAR

- 从 [Releases](https://github.com/1026zxl/openai-code-review/releases) 下载 `openai-code-review-sdk-1.0-SNAPSHOT.jar`（或当前版本），或  
- 在本仓库执行：`mvn clean package -pl openai-code-review-sdk`，产物位于 `openai-code-review-sdk/target/openai-code-review-sdk-1.0-SNAPSHOT.jar`。

### 4.2 运行

在**已 clone 且存在最近一次提交**的 Git 仓库根目录下执行：

```bash
java -jar openai-code-review-sdk-1.0-SNAPSHOT.jar
```

通过环境变量传入必要配置（见下节）。评审完成后，报告会写入当前目录下的 `代码评审记录/`（或 `CODE_REVIEW_REPORT_DIR` 指定目录），并可根据配置上传到 GitHub 仓库。

---

## 五、方式三：GitHub Actions 集成

在目标仓库的 `.github/workflows/` 下新增或复用 workflow，在 push/PR 时自动跑评审。

### 5.1 示例 Workflow

```yaml
name: AI Code Review
on:
  push:
    branches: ['*']
  pull_request:
    branches: ['*']

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 2

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: 11
          distribution: 'temurin'

      - name: Create libs directory
        run: mkdir -p ./libs

      - name: Download openai-code-review-sdk JAR
        run: >
          wget -O ./libs/openai-code-review-sdk-1.0-SNAPSHOT.jar
          https://github.com/1026zxl/openai-code-review/releases/download/open-ai-code-review.jar/openai-code-review-sdk-1.0-SNAPSHOT.jar

      - name: Run openai-code-review
        run: java -jar ./libs/openai-code-review-sdk-1.0-SNAPSHOT.jar
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          CODE_TOKEN: ${{ secrets.CODE_TOKEN }}
          # 可选：微信公众号通知
          WECHAT_APP_ID: ${{ secrets.WECHAT_APP_ID }}
          WECHAT_APP_SECRET: ${{ secrets.WECHAT_APP_SECRET }}
          WECHAT_TEMPLATE_ID: ${{ secrets.WECHAT_TEMPLATE_ID }}
          WECHAT_OPEN_ID: ${{ secrets.WECHAT_OPEN_ID }}
```

### 5.2 仓库需配置的 Secrets

- **OPENAI_API_KEY**（必填）：大模型 API 密钥（如阿里云 DashScope）
- **CODE_TOKEN**（可选）：若需将报告推送至 GitHub 仓库，填具有写权限的 GitHub Token；环境变量名可通过配置修改，默认 `CODE_TOKEN`。

其余如 `WECHAT_*` 仅在使用微信公众号通知时需要。

---

## 六、配置说明

### 6.1 环境变量（推荐在 CI/运维侧使用）

| 变量名 | 必填 | 说明 |
|--------|------|------|
| `OPENAI_API_KEY` | 是 | 大模型 API Key（默认读取此变量，可通过配置修改） |
| `CODE_REVIEW_API_URL` | 否 | API 地址，默认阿里云 DashScope 兼容端点 |
| `CODE_REVIEW_MODEL` | 否 | 模型名，默认 `qwen-flash` |
| `CODE_REVIEW_REPORT_DIR` | 否 | 报告根目录，默认 `代码评审记录` |
| `CODE_REVIEW_GITHUB_REPO_URL` | 否 | 评审报告要推送到的 GitHub 仓库 URL |
| `CODE_TOKEN` | 否 | 推送报告到 GitHub 时使用的 Token（可改为其他名） |

微信公众号相关：`WECHAT_APP_ID`、`WECHAT_APP_SECRET`、`WECHAT_TEMPLATE_ID`、`WECHAT_OPEN_ID`，详见下方「可选：微信公众号通知」。

### 6.2 配置文件 `code-review.properties`

可将 `openai-code-review-sdk/src/main/resources/code-review.properties.example` 复制为 `code-review.properties` 并放在 classpath 或指定路径，主要项示例：

```properties
# 必填
code.review.api.key=your_api_key_here

# 可选
code.review.api.url=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
code.review.model=qwen-flash
code.review.report.base.dir=代码评审记录
code.review.git.repository.path=/path/to/repo
code.review.github.repo.url=https://github.com/your-org/code-review-repo.git
code.review.github.token.env=CODE_TOKEN
```

配置文件与环境变量可同时使用，环境变量会覆盖同名配置。

### 6.3 Builder 参数一览

通过 `CodeReviewClient.builder()` 可设置：`apiKey`、`apiUrl`、`model`、`temperature`、`maxTokens`、`reportBaseDir`、`gitRepositoryPath`、`wechatAppId`、`wechatAppSecret`、`wechatTemplateId`、`wechatOpenId`、`wechatEnabled` 等，与配置项一一对应。

---

## 七、评审结果与报告

- **返回值**：`ReviewResult` 包含 `getReportPath()`、`getReviewContent()`、`getReviewTime()`、`getCodeInfo()` 等，便于在流水线中判断是否通过或仅做记录。
- **报告位置**：默认在 `代码评审记录/提交人/日期/提交描述 - 提交人.md`，也可上传到配置的 GitHub 仓库。
- **评审维度**：技术正确性与逻辑、安全与可靠性、性能与可扩展性、代码风格与可维护性、可测试性等。

---

## 八、可选：微信公众号通知

若希望评审结束后将结果推送到企业微信/公众号，可配置：

- 环境变量：`WECHAT_APP_ID`、`WECHAT_APP_SECRET`、`WECHAT_TEMPLATE_ID`、`WECHAT_OPEN_ID`
- 或配置文件：`code.review.wechat.*` 及 `code.review.wechat.enabled=true`

配置完整且启用后，SDK 会在评审完成后发送模板消息，无需改代码。

---

## 九、依赖与运行环境

- **JDK**：8 及以上（推荐 11+，与 CI 保持一致）。
- **运行时**：SDK 使用 SLF4J 日志门面；若通过 Maven 引入，请确保 classpath 中有任意 SLF4J 实现（如 `slf4j-simple`、`logback-classic`）。使用 fat JAR 时，JAR 内已带部分依赖，可直接运行。
- **Git**：执行评审的目录需为 Git 仓库，且存在至少一次提交（会针对最近一次提交的变更进行评审）。

---

## 十、常见问题

1. **报错 API Key 缺失**  
   请设置环境变量 `OPENAI_API_KEY` 或在配置文件/Builder 中指定 `apiKey`。

2. **报告未上传到 GitHub**  
   检查是否配置 `CODE_TOKEN`（或 `code.review.github.token`）以及 `CODE_REVIEW_GITHUB_REPO_URL`，且 Token 对目标仓库有写权限。

3. **没有产生 diff / 评审内容为空**  
   确认当前目录是 Git 仓库根目录，且已有至少一次提交；若在 CI 中，确保 `checkout` 时 `fetch-depth` 足够（例如 2）。

4. **想改用其他兼容 OpenAI 的 API**  
   通过 `apiUrl` 和 `model` 指定你的端点与模型名即可，无需改代码。

---

## 十一、小结

- **减少人工评审成本**：在 MR/PR 或每次 push 后自动跑一轮 AI 评审，报告可归档或通知到人。
- **接入三选一**：Maven + 编程调用、JAR 命令行、GitHub Actions；按团队技术栈与 CI 选择即可。
- **配置灵活**：环境变量、`code-review.properties`、Builder 三种方式可组合，密钥建议用环境变量或 Secrets，不要写进代码或提交到仓库。

如有问题或需求，欢迎提 Issue 或联系维护团队。
