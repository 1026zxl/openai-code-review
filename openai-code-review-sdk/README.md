# OpenAI代码自动评审组件

## 功能概述

这是一个基于OpenAI的自动化代码评审组件，能够在代码提交时自动进行代码质量检查和评审。

## 核心功能

1. **自动检出代码变更** - 使用JGit检测最近一次提交的代码差异
2. **AI代码评审** - 调用阿里云Qwen-Flash模型进行智能代码评审
3. **结构化报告** - 生成详细的代码评审报告，包含问题分类和改进建议
4. **评审记录管理** - 自动将评审结果上传到 GitHub 仓库，格式为 Markdown

## 使用方法

### 1. 环境配置

设置环境变量：
```bash
export OPENAI_API_KEY="your_api_key_here"
export GITHUB_TOKEN="your_github_token_here"  # 用于上传评审报告到 GitHub
```

**注意**：GitHub Token 需要具有对目标仓库的写入权限。也可以通过配置文件 `code-review.properties` 设置。

### 2. 运行评审

```bash
# 编译项目
mvn clean install

# 运行代码评审
java -jar openai-code-review-sdk/target/openai-code-review-sdk-1.0-SNAPSHOT.jar
```

### 3. GitHub Actions集成

项目已配置GitHub Actions工作流，当代码推送到GitHub时会自动触发评审。

**配置 GitHub Secrets**：
- `OPENAI_API_KEY`: OpenAI API 密钥
- `GITHUB_TOKEN`: GitHub Token（用于上传评审报告）

## 输出结构

评审报告会自动上传到配置的 GitHub 仓库（默认：`https://github.com/1026zxl/code-review-repository.git`），目录结构如下：
```
代码评审记录/
├── 提交人名称/
│   ├── 2026-01-12/
│   │   ├── 提交描述 - 提交人名称.md
│   │   └── ...
│   └── 2026-01-13/
│       └── ...
└── ...
```

报告格式为 **Markdown**，包含评审时间、提交信息、评审结果等结构化内容。

## 评审维度

AI评审会从以下维度分析代码：

- **技术正确性与逻辑** - 功能实现、边界条件、错误处理
- **安全性与可靠性** - 安全漏洞、资源管理、日志记录
- **性能与可扩展性** - 性能瓶颈、扩展性考虑
- **代码风格与可维护性** - 编码规范、命名、注释
- **可测试性** - 单元测试友好性

## 技术栈

- Java 8
- JGit - Git操作
- OkHttp - HTTP客户端
- FastJSON - JSON处理
- SLF4J - 日志框架
- GitHub Actions - CI/CD