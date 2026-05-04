# 行测训练 UI 重设计规格文档

> 日期：2026-05-04
> 范围：XingceView（模块选择页）+ XingceModuleView（答题页）
> 风格：极简黑白风，对齐 PracticeView 设计语言

---

## 1. 设计系统（Design Tokens）

全局 CSS 变量，定义在 `vue-project/src/styles/tokens.css`，两个页面统一引用。

### 1.1 8pt 空间律

所有间距为 8 的倍数：

| Token | 值 |
|---|---|
| `--space-1` | 8px |
| `--space-2` | 16px |
| `--space-3` | 24px |
| `--space-4` | 32px |
| `--space-5` | 40px |
| `--space-6` | 48px |

### 1.2 文字层级

| 层级 | Token | 颜色 | 字重 | 用途 |
|---|---|---|---|---|
| 主标题 | `--color-text-title` | `#1D2129` | 600 | 模块名、题号 |
| 正文 | `--color-text-body` | `#4E5969` | 400-500 | 题干、选项文字 |
| 副标题 | `--color-text-caption` | `#86909C` | 400 | 模块描述、统计 |
| 禁用 | `--color-text-disabled` | `#C9CDD4` | 400 | 不可用态 |

### 1.3 背景与阴影（去线留影）

| Token | 值 |
|---|---|
| `--color-bg-page` | `#F7F8FA` |
| `--color-bg-card` | `#FFFFFF` |
| `--shadow-card` | `0 4px 20px rgba(0,0,0,0.04)` |
| `--shadow-card-hover` | `0 8px 30px rgba(0,0,0,0.08)` |

### 1.4 圆角

| Token | 值 | 用途 |
|---|---|---|
| `--radius-card` | 16px | 大卡片 |
| `--radius-btn` | 12px | 按钮、选项 |
| `--radius-full` | 999px | 胶囊按钮 |

### 1.5 字体栈

```css
--font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
--font-mono: ui-monospace, "SF Mono", "Cascadia Code", monospace;
```

---

## 2. 模块选择页（XingceView）

### 2.1 布局

- 2x2 Grid 布局，`grid-template-columns: repeat(2, 1fr)`
- 最大宽度 1100px，水平居中
- 页面底色 `--color-bg-page`（`#F7F8FA`）
- Grid 间距 `--space-3`（24px）

### 2.2 卡片设计

**结构**（从上到下）：

```
┌─────────────────────────────┐
│  ┌──────┐                   │
│  │ 📊  │  ← 48px SVG 图标   │
│  └──────┘    + 浅色 Blob 背景│
│                             │
│  常识                       │  ← 16px / 600 / #1D2129
│  政治、经济、法律、科技、历史 │  ← 13px / 400 / #86909C
│                             │  ← 双行截断
│  ─────────────────────────  │  ← 细分割线 #f0f0f0
│  128 道题 · 正确率 72%      │  ← 12px / 400 / #C9CDD4
└─────────────────────────────┘
```

**样式规范**：

- 无边框（`border: none`）
- 弥散阴影：`--shadow-card`
- 圆角：`--radius-card`（16px）
- 内边距：24px
- 图标：48px SVG 线性图标，背后垫浅色不规则 Blob 背景（`rgba(0,0,0,0.03)` 渐变圆）
- 副标题：双行截断（`-webkit-line-clamp: 2`）保证卡片等高
- 统计信息：用 `border-top: 1px solid #f0f0f0` 与主体信息隔离

**交互**：

- hover：阴影升级为 `--shadow-card-hover` + `translateY(-2px)`，过渡 150ms `cubic-bezier(0.4,0,0.2,1)`
- active：`scale(0.98)`，150ms

### 2.3 图标方案

用 SVG 线性图标替代纯 emoji，每个模块对应一个 48px 图标：

| 模块 | 图标描述 |
|---|---|
| 常识 | 书本/灯泡线性图标 |
| 言语理解 | 对话气泡线性图标 |
| 资料分析 | 柱状图线性图标 |
| 推理判断 | 拼图/逻辑线性图标 |
| 数量关系 | 数字/计算器线性图标 |

图标背后垫一个 `64x64` 的浅灰色圆形 Blob 背景（`background: radial-gradient(circle, rgba(0,0,0,0.03) 0%, transparent 70%)`）。

---

## 3. 答题页（XingceModuleView）

### 3.1 顶部栏

对齐 PracticeView 的 topbar 风格：

```
[← 返回]  资料分析  ████████░░ 4/10    [本题 12s] [总用时 02:34]
```

- 左侧：返回按钮（`border: 1px solid #eee`，`border-radius: 8px`）+ 模块名 + 进度条
- 右侧：两个计时器芯片（浅灰底 + 黑底白字）
- 进度条：4px 高，`#111` 填充色

### 3.2 题目卡片

- 无边框，弥散阴影，16px 圆角
- 内边距 32px 28px
- 题号 badge：`Q1`，浅灰底，`72rem/700`
- 难度星级 + 来源标签（保留现有逻辑）
- **题干文字**：`18px / 400 / line-height 1.7 / #1D2129`
  - 字重 400（长文本友好，减少视觉疲劳）
  - 关键词/数据可加粗 600
  - 资料分析模块的大段材料必须 400

### 3.3 选项

对齐 PracticeView 的 opt-btn 风格：

```
┌─────────────────────────────────┐
│ [A]  选项文字内容               ✓│
└─────────────────────────────────┘
```

- 浅灰底 `#fafafa`，边框 `1px solid #eee`
- hover：边框加深 `#ddd`，微上浮 1px
- **选中态**：黑色边框 1.5px + 字母圆圈变黑底白字
- **选中动画**：`scale(0.98) → scale(1)`，150ms `cubic-bezier(0.34,1.56,0.64,1)`（弹性效果）
- **正确态**：`#d1fae5` 背景 + `#10b981` 边框
- **错误态**：`#fee2e2` 背景 + `#ef4444` 边框

### 3.4 解析区

- 技能标签：黑底白字 pill（`--radius-full`）
- 解析文字：`14px / 400 / #4E5969 / line-height 1.7`
- 陷阱警示卡片：保留现有逻辑，样式对齐（无边框、浅红底、16px 圆角）

### 3.5 底部按钮

胶囊形，对齐 PracticeView：

- 未激活：`#eee` 底 + `#bbb` 字，`cursor: not-allowed`
- 激活：`#111` 底 + `#fff` 字，hover 时 `#333` + 微上浮 + 阴影

### 3.6 空状态 & AI 出题

- 空状态：居中图标 + 提示文字 + AI 出题按钮
- AI 出题中：spinner + "AI 出题中..." 文字

---

## 4. 情感化微交互

| 交互 | 动画 | 时长 |
|---|---|---|
| 卡片 hover | `translateY(-2px)` + 阴影加深 | 150ms |
| 卡片 active | `scale(0.98)` | 150ms |
| 选项选中 | `scale(0.98→1)` 弹性 | 150ms |
| 页面切换 | fade + translateY(16px) | 300ms |
| 题目切换 | slide-fade（左右滑入） | 250ms |

---

## 5. 响应式策略

| 断点 | 布局调整 |
|---|---|
| ≥1100px | 2x2 Grid，max-width 1100px |
| 768px-1099px | 2x2 Grid，内边距缩减 |
| <768px | 单列，内边距 16px，卡片全宽 |
| <640px | 答题页：topbar 纵向排列，选项文字缩小 |

### 5.1 文本截断

副标题统一使用双行截断，保证卡片等高：

```css
.text-truncate-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

---

## 6. 技术实现要点

### 6.1 文件结构

```
vue-project/src/
├── styles/
│   └── tokens.css          ← 新建：设计系统变量
├── views/
│   ├── XingceView.vue      ← 重写：模块选择页
│   └── XingceModuleView.vue ← 重写：答题页
```

### 6.2 引入方式

在 `main.js` 或 `App.vue` 中 `import './styles/tokens.css'`，全局生效。

### 6.3 交互逻辑

- 两个页面的业务逻辑（API 调用、状态管理、答题流程）保持不变
- 仅重写 template 和 style 部分
- script 部分按需微调（如添加动画控制 ref）
