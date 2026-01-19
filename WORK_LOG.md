## 说明

本文件用于记录本次工作上下文与已完成事项，供下次继续任务时参考。

## 已完成事项

### 前端（boj-backend\frontend）

- 新增语言选项并与后端沙箱统一：`java` / `gcc` / `javascript` / `python`
- 题目详情页代码模板改为 A + B，并覆盖上述语言
- 提交列表与详情页语言展示统一格式化（新增 `formatLanguage`）
- 语言筛选与显示使用 `gcc` 表示 C

关键文件：
- `frontend/src/pages/QuestionDetail.vue`
- `frontend/src/pages/SubmissionList.vue`
- `frontend/src/pages/SubmissionDetail.vue`
- `frontend/src/utils/format.js`

### 后端（boj-backend）

- 判题输出比较规则：忽略每行行尾空格/制表符，忽略末尾多余空行
- 涉及策略类：`DefaultJudgeStrategy` / `JavaLanguageJudgeStrategy`

### 沙箱（boj-sandbox）

- C 语言改为 Docker 编译与 Docker 运行
- Java 语言编译也迁移到 Docker
- 抽取 Docker 模板类 `DockerCodeSandboxTemplate`，Java/C 共用
- 新增 `ExecResult` 作为 Docker exec 结果模型
- 抽取 Docker 连接工厂 `DockerClientFactory`（支持 `DOCKER_HOST`）
- 输出归一化逻辑统一放在 `DockerCodeSandboxTemplate.getOutputResponse`

关键文件：
- `sandbox/src/main/java/com/bin/sandbox/DockerCodeSandboxTemplate.java`
- `sandbox/src/main/java/com/bin/sandbox/GccDockerCodeSandbox.java`
- `sandbox/src/main/java/com/bin/sandbox/JavaDockerCodeSandbox.java`
- `sandbox/src/main/java/com/bin/sandbox/config/DockerClientFactory.java`
- `sandbox/src/main/java/com/bin/sandbox/model/ExecResult.java`

### 镜像与目录结构

新增镜像目录：
- `sandbox/docker/java/Dockerfile`（openjdk:8u342-jdk-slim-buster）
- `sandbox/docker/gcc/Dockerfile`（gcc:13）
- `sandbox/docker/python/Dockerfile`（python:3.11-slim）
- `sandbox/docker/javascript/Dockerfile`（node:18-bullseye-slim）

## 判题输出规则

与洛谷一致：对比输出时
- 忽略每行末尾空格与制表符
- 忽略最后一行之后的多余空行
- 其它内容必须完全一致

## 当前状态

- 代码已分别提交并推送到：
  - `boj-backend`（提示仓库已迁移到 `https://github.com/Thinkya1/boj-backend.git`）
  - `boj-sandbox`
- `boj-backend` 工作区存在未跟踪文件（如 `.DS_Store`、`sql/patch_question_number.sql`、`src/boj-backend.lnk`）

## 待办与建议

- 如需 Python/JavaScript 沙箱运行能力：需要补相应 Docker 沙箱类并接入 `CodeSandboxManager`
- 如需统一镜像构建与版本管理：可增加构建脚本或 CI
