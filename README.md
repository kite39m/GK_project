# 🎯 GK-Diagnosis-Agent (国考行测速算智能推演系统)

> 💡 **项目愿景**：打破传统公务员考试备考中“资料分析”模块的题海战术，通过多 Agent 协同，为考生提供针对“截位直除”、“差分法”等速算技巧的个性化长链推演与纠错。

**👤 开发者**: 赵文阳 

## 🏗️ 核心架构与技术栈
本项目采用前后端分离架构，核心业务流由 AI大模型节点驱动：
*   **前端交互**: Vue 3 + Vite + Vue Router (提供草稿录入与富文本推演展示)
*   **后端服务**: Spring Boot + MyBatis-Plus + JWT 鉴权
*   **AI 调度层**: 规划中的多 Agent 协作网络 (Diagnostic Agent, Strategy Agent, Variant Agent)

## 🧠 多 Agent 核心逻辑流 (核心功能实现中)
1.  **深度归因 (Diagnostic)**: 接收用户在 `PracticeView` 中输入的错题与草稿步骤，逆向推演用户的计算卡壳点（例如：估算精度不足导致误选）。
2.  **策略匹配 (Strategy)**: 结合内置公考知识库，输出针对性的纠错解析。
3.  **动态变式 (Variant)**: 动态生成同等难度、同类陷阱的衍生题目，巩固薄弱项。

## 🚀 阶段性规划 (Roadmap)
- [x] Phase 1: 基础题库系统搭建 (用户鉴权、题目分类、错题本基础 CRUD)
- [x] Phase 2: JWT 安全链路与前后端联调测试
- [ ] **Phase 3 (Current)**: 接入高阶大模型 API 进行长链推理能力测试。**下一步计划全面接入小米 MiMo-V2.5-Pro API (MiMo Orbit 计划) ，利用其强大的上下文理解能力，重构核心的错题逆向诊断逻辑。**
- [ ] Phase 4: 部署上线并进行小规模内测。

## ⚙️ 本地运行指南
1. 后端：配置 `application.yml` 中的 MySQL 连接，运行 `GkBackendApplication.java`。
2. 前端：进入 `vue-project` 目录，执行 `npm install` 与 `npm run dev`。
