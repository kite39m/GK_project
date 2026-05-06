# 题目图片采集与展示设计文档

## 概述

在采集题目时，同时抓取题目中的图片（包括题目内嵌图片和解析配图），并在选项中展示图片，让题目更生动形象。

### 核心目标

- 采集题目时同时提取图片 URL
- 智能筛选：只提取与题目相关的图片
- 图片验证：检查格式和可访问性
- 内容哈希去重：避免重复图片
- 数据库缓存：存储图片信息
- 前端展示：在选项中显示图片

### 技术栈

| 组件 | 技术 |
|------|------|
| 图片提取 | Jsoup (HTML 解析) |
| 后端 | Spring Boot 3.5.13 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL (utf8mb4) |
| 前端 | Vue 3 + Composition API |

---

## 第 1 节：数据库设计

### question_image 表

```sql
CREATE TABLE question_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    url VARCHAR(500) NOT NULL COMMENT '图片URL',
    content_hash VARCHAR(64) NOT NULL COMMENT '图片内容哈希（用于去重）',
    source_page VARCHAR(500) COMMENT '来源页面URL',
    image_type VARCHAR(20) DEFAULT 'OPTION' COMMENT '图片类型: OPTION/ANALYSIS/TITLE',
    format VARCHAR(10) COMMENT '图片格式: jpg/png/gif',
    file_size BIGINT COMMENT '文件大小（字节）',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INVALID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_hash (content_hash),
    INDEX idx_source (source_page)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目图片缓存表';
```

### Entity 类

```java
@TableName("question_image")
@Data
public class QuestionImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String contentHash;
    private String sourcePage;
    private String imageType;
    private String format;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String status;
    private Date createdAt;
}
```

### Mapper

```java
@Mapper
public interface QuestionImageMapper extends BaseMapper<QuestionImage> {
}
```

---

## 第 2 节：ImageExtractor 组件设计

### 职责

负责图片的提取、筛选、验证和去重。

### 核心方法

```java
@Component
public class ImageExtractor {

    @Autowired
    private QuestionImageMapper imageMapper;

    /**
     * 从 HTML 页面提取图片 URL
     */
    public List<String> extractFromPage(String html, String sourceUrl) {
        // 1. 使用 Jsoup 解析 HTML
        // 2. 提取所有 img 标签的 src 属性
        // 3. 智能筛选：过滤装饰性图片
        // 4. 验证格式和大小
        // 5. 内容哈希去重
        // 6. 缓存到数据库
    }

    /**
     * 从选项 JSON 中提取图片 URL
     */
    public List<String> extractFromOptions(String optionsJson) {
        // 解析 JSON，提取值中的 URL
    }

    /**
     * 智能筛选：判断图片是否与题目相关
     */
    private boolean isRelevantImage(String url, String alt, int width, int height) {
        // 1. 过滤小图标（< 50x50）
        // 2. 过滤装饰性图片（logo、背景等）
        // 3. 保留图表、图形、公式等题目相关图片
    }

    /**
     * 验证图片格式和大小
     */
    private boolean validateImage(String url) {
        // 1. 检查 URL 是否可访问
        // 2. 检查 Content-Type 是否为图片
        // 3. 检查格式是否为 jpg/png/gif
    }

    /**
     * 计算图片内容哈希
     */
    private String calculateHash(String url) {
        // 下载图片前 1KB 计算 MD5
        // 或使用 URL 本身计算哈希
    }
}
```

### 智能筛选规则

| 规则 | 条件 | 处理 |
|------|------|------|
| 小图标 | width < 50 或 height < 50 | 过滤 |
| 装饰性图片 | alt 包含 "logo"、"icon"、"bg" 等 | 过滤 |
| 题目相关图片 | 包含图表、图形、公式等关键词 | 保留 |
| 默认 | 其他情况 | 保留 |

### 验证规则

| 检查项 | 规则 | 处理 |
|--------|------|------|
| URL 可访问 | HTTP 状态码 200 | 通过 |
| Content-Type | image/jpeg, image/png, image/gif | 通过 |
| 文件格式 | jpg, png, gif | 通过 |
| 文件大小 | 不限制 | 通过 |
| URL 失效 | 定期检查，状态码非 200 | 标记为 INVALID |

---

## 第 3 节：与采集流程集成

### QuestionCollectorAgent 修改

```java
@AiService
public interface QuestionCollectorAgent {

    @SystemMessage("""
        你是国考行测真题采集专家。
        
        采集流程：
        1. 使用 webSearch 搜索真题
        2. 使用 fetchPage 抓取页面内容
        3. 使用 extractImages 提取图片 URL
        4. 解析题目、选项、答案、解析
        5. 将图片 URL 嵌入选项 JSON
        6. 使用 saveQuestion 保存题目
        
        选项格式要求：
        - 文字选项：{"A": "选项文本"}
        - 图片选项：{"A": "![image](https://xxx.jpg)"}
        - 混合选项：{"A": "文本 ![image](https://xxx.jpg)"}
        
        注意：
        - 只提取与题目相关的图片
        - 图片 URL 必须可访问
        - 选项中的图片用 Markdown 格式
    """)
    String collectQuestions(@UserMessage String prompt);
}
```

### 采集流程

```
用户触发采集
    ↓
QuestionCollectorAgent
    ↓
WebSearchTool (搜索真题)
    ↓
PageFetchTool (抓取页面)
    ↓
ImageExtractor (提取图片)
    ↓
AI 解析题目结构
    ↓
图片 URL 嵌入选项 JSON
    ↓
校验 + 去重
    ↓
存入 ai_question_pool
```

### 选项 JSON 格式

```json
{
  "A": "![正方形](https://example.com/square.jpg)",
  "B": "![圆形](https://example.com/circle.jpg)",
  "C": "![三角形](https://example.com/triangle.jpg)",
  "D": "![长方形](https://example.com/rectangle.jpg)"
}
```

---

## 第 4 节：前端展示设计

### 选项解析逻辑

```javascript
// 解析选项文本，识别图片标记
function parseOptionText(text) {
  // 匹配 Markdown 图片格式：![alt](url)
  const imageRegex = /!\[.*?\]\((.*?)\)/g;
  const parts = [];
  let lastIndex = 0;
  let match;

  while ((match = imageRegex.exec(text)) !== null) {
    // 添加图片前的文字
    if (match.index > lastIndex) {
      parts.push({
        type: 'text',
        content: text.slice(lastIndex, match.index)
      });
    }
    // 添加图片
    parts.push({
      type: 'image',
      url: match[1]
    });
    lastIndex = match.index + match[0].length;
  }

  // 添加剩余文字
  if (lastIndex < text.length) {
    parts.push({
      type: 'text',
      content: text.slice(lastIndex)
    });
  }

  return parts;
}
```

### 选项展示组件

```vue
<template>
  <div class="option-item" @click="selectOption">
    <span class="option-label">{{ label }}.</span>
    <div class="option-content">
      <template v-for="(part, index) in parsedParts" :key="index">
        <span v-if="part.type === 'text'">{{ part.content }}</span>
        <img 
          v-else-if="part.type === 'image'" 
          :src="part.url" 
          :alt="'选项图片'"
          class="option-image"
          @load="onImageLoad"
          @error="onImageError"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.option-image {
  max-width: 100%;
  max-height: 200px;
  margin: 8px 0;
  border-radius: 4px;
  border: 1px solid #eee;
}
</style>
```

### 错误处理

```javascript
// 图片加载失败时的处理
function onImageError(e) {
  e.target.style.display = 'none';
  // 显示占位文本
  const placeholder = document.createElement('span');
  placeholder.textContent = '[图片加载失败]';
  placeholder.className = 'image-error';
  e.target.parentNode.appendChild(placeholder);
}
```

---

## 第 5 节：涉及文件清单

### 新增

| 文件 | 说明 |
|------|------|
| `db/migration/V6__add_question_image_table.sql` | 图片缓存表 DDL |
| `entity/QuestionImage.java` | 图片实体类 |
| `mapper/QuestionImageMapper.java` | 图片 Mapper |
| `ai/tools/ImageExtractor.java` | 图片提取组件 |

### 修改

| 文件 | 说明 |
|------|------|
| `ai/collector/QuestionCollectorAgent.java` | 增加图片提取步骤 |
| `ai/tools/PageFetchTool.java` | 增加图片提取方法 |
| `vue-project/src/components/QuestionOption.vue` | 支持图片展示 |
| `vue-project/src/utils/optionParser.js` | 选项解析工具 |

---

## 第 6 节：实施阶段

### Phase A：数据库 + Entity + Mapper
- DDL 建表
- QuestionImage Entity + QuestionImageMapper

### Phase B：ImageExtractor 组件
- 实现图片提取、筛选、验证、去重逻辑
- 单元测试

### Phase C：采集流程集成
- 修改 QuestionCollectorAgent
- 修改 PageFetchTool
- 集成测试

### Phase D：前端展示
- 实现选项解析逻辑
- 实现图片展示组件
- 错误处理
- UI 测试

---

## 第 7 节：注意事项

### 图片版权
- 只采集公开可访问的图片
- 保存图片来源信息
- 遵守网站的 robots.txt 规则

### 性能考虑
- 图片验证可能较慢，考虑异步处理
- 大量图片时考虑批量处理
- 缓存机制减少重复提取
- 图片提取超时设置：单个图片 5 秒，整体 30 秒

### 错误处理
- 图片加载失败时显示占位文本
- 图片 URL 失效时的处理：标记为 INVALID，下次采集时重新提取
- 网络超时的处理：重试 3 次，失败后跳过
- 图片缓存更新策略：每周检查一次 URL 有效性
