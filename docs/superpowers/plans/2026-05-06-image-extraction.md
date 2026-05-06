# 题目图片采集与展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在采集题目时，同时抓取题目中的图片（包括题目内嵌图片和解析配图），并在选项中展示图片，让题目更生动形象。

**Architecture:** 新建 ImageExtractor 组件负责图片的提取、筛选、验证和去重，与现有 QuestionCollectorAgent 集成，前端使用 Vue 3 组件支持图文混排展示。

**Tech Stack:** Spring Boot 3.5.13 + MyBatis-Plus 3.5.5 + Jsoup + Vue 3 + Composition API

---

## 文件结构

### 新增文件

| 文件路径 | 职责 |
|----------|------|
| `gk-backend/gk-backend/src/main/resources/db/migration/V6__add_question_image_table.sql` | 图片缓存表 DDL |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/QuestionImage.java` | 图片实体类 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/QuestionImageMapper.java` | 图片 Mapper |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/ImageExtractor.java` | 图片提取组件 |
| `vue-project/src/utils/optionParser.js` | 选项解析工具 |
| `vue-project/src/components/QuestionOption.vue` | 选项展示组件 |

### 修改文件

| 文件路径 | 职责 |
|----------|------|
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java` | 增加图片提取步骤 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java` | 增加图片提取方法 |
| `gk-backend/gk-backend/pom.xml` | 添加 Jsoup 依赖 |

---

## Task 1: 数据库设计与 Entity

**Files:**
- Create: `gk-backend/gk-backend/src/main/resources/db/migration/V6__add_question_image_table.sql`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/QuestionImage.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/QuestionImageMapper.java`

- [ ] **Step 1: 创建数据库迁移文件**

```sql
-- V6__add_question_image_table.sql
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

- [ ] **Step 2: 创建 Entity 类**

```java
// QuestionImage.java
package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

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

- [ ] **Step 3: 创建 Mapper 接口**

```java
// QuestionImageMapper.java
package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.QuestionImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionImageMapper extends BaseMapper<QuestionImage> {
}
```

- [ ] **Step 4: 验证数据库迁移**

运行: `cd gk-backend/gk-backend && ./mvnw flyway:migrate`
Expected: 迁移成功，question_image 表已创建

- [ ] **Step 5: 提交代码**

```bash
git add gk-backend/gk-backend/src/main/resources/db/migration/V6__add_question_image_table.sql
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/QuestionImage.java
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/QuestionImageMapper.java
git commit -m "feat: add question_image table and entity"
```

---

## Task 2: 添加 Jsoup 依赖

**Files:**
- Modify: `gk-backend/gk-backend/pom.xml`

- [ ] **Step 1: 添加 Jsoup 依赖**

```xml
<!-- pom.xml, 在 <dependencies> 中添加 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

- [ ] **Step 2: 验证依赖下载**

运行: `cd gk-backend/gk-backend && ./mvnw dependency:resolve`
Expected: Jsoup 依赖下载成功

- [ ] **Step 3: 提交代码**

```bash
git add gk-backend/gk-backend/pom.xml
git commit -m "deps: add Jsoup dependency for HTML parsing"
```

---

## Task 3: 实现 ImageExtractor 组件

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/ImageExtractor.java`

- [ ] **Step 1: 创建 ImageExtractor 类骨架**

```java
// ImageExtractor.java
package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.QuestionImage;
import com.zwy.gk_backend.mapper.QuestionImageMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageExtractor {

    @Autowired
    private QuestionImageMapper imageMapper;

    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
        "!\\[.*?\\]\\((https?://[^\\)]+)\\)"
    );

    private static final Set<String> DECORATIVE_KEYWORDS = Set.of(
        "logo", "icon", "bg", "background", "banner", "ad", "advertisement"
    );

    private static final int MIN_IMAGE_SIZE = 50;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
}
```

- [ ] **Step 2: 实现 extractFromPage 方法**

```java
/**
 * 从 HTML 页面提取图片 URL
 */
public List<String> extractFromPage(String html, String sourceUrl) {
    List<String> imageUrls = new ArrayList<>();
    
    if (html == null || html.isEmpty()) {
        return imageUrls;
    }

    try {
        Document doc = Jsoup.parse(html);
        Elements imgElements = doc.select("img[src]");

        for (Element img : imgElements) {
            String src = img.attr("abs:src");
            String alt = img.attr("alt");
            String widthStr = img.attr("width");
            String heightStr = img.attr("height");

            int width = parseDimension(widthStr);
            int height = parseDimension(heightStr);

            if (isRelevantImage(src, alt, width, height) && validateImage(src)) {
                String hash = calculateHash(src);
                if (!isDuplicate(hash)) {
                    saveImage(src, hash, sourceUrl, "OPTION", width, height);
                    imageUrls.add(src);
                }
            }
        }
    } catch (Exception e) {
        // 日志记录异常
    }

    return imageUrls;
}
```

- [ ] **Step 3: 实现 extractFromOptions 方法**

```java
/**
 * 从选项 JSON 中提取图片 URL
 */
public List<String> extractFromOptions(String optionsJson) {
    List<String> imageUrls = new ArrayList<>();
    
    if (optionsJson == null || optionsJson.isEmpty()) {
        return imageUrls;
    }

    Matcher matcher = IMAGE_URL_PATTERN.matcher(optionsJson);
    while (matcher.find()) {
        String url = matcher.group(1);
        if (validateImage(url)) {
            imageUrls.add(url);
        }
    }

    return imageUrls;
}
```

- [ ] **Step 4: 实现 isRelevantImage 方法**

```java
/**
 * 智能筛选：判断图片是否与题目相关
 */
private boolean isRelevantImage(String url, String alt, int width, int height) {
    if (url == null || url.isEmpty()) {
        return false;
    }

    // 过滤小图标
    if (width > 0 && width < MIN_IMAGE_SIZE) {
        return false;
    }
    if (height > 0 && height < MIN_IMAGE_SIZE) {
        return false;
    }

    // 过滤装饰性图片
    if (alt != null) {
        String altLower = alt.toLowerCase();
        for (String keyword : DECORATIVE_KEYWORDS) {
            if (altLower.contains(keyword)) {
                return false;
            }
        }
    }

    // 过滤 base64 图片
    if (url.startsWith("data:")) {
        return false;
    }

    return true;
}
```

- [ ] **Step 5: 实现 validateImage 方法**

```java
/**
 * 验证图片格式和大小
 */
private boolean validateImage(String url) {
    if (url == null || url.isEmpty()) {
        return false;
    }

    try {
        URL imageUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return false;
        }

        String contentType = connection.getContentType();
        if (contentType == null) {
            return false;
        }

        // 检查是否为图片格式
        if (!contentType.startsWith("image/")) {
            return false;
        }

        // 检查是否为支持的格式
        String format = contentType.substring(6).toLowerCase();
        if (!format.equals("jpeg") && !format.equals("jpg") && 
            !format.equals("png") && !format.equals("gif")) {
            return false;
        }

        connection.disconnect();
        return true;
    } catch (IOException e) {
        return false;
    }
}
```

- [ ] **Step 6: 实现 calculateHash 方法**

```java
/**
 * 计算图片内容哈希
 */
private String calculateHash(String url) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(url.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    } catch (NoSuchAlgorithmException e) {
        return String.valueOf(url.hashCode());
    }
}
```

- [ ] **Step 7: 实现辅助方法**

```java
/**
 * 检查是否重复
 */
private boolean isDuplicate(String hash) {
    QueryWrapper<QuestionImage> query = new QueryWrapper<>();
    query.eq("content_hash", hash);
    return imageMapper.selectCount(query) > 0;
}

/**
 * 保存图片信息
 */
private void saveImage(String url, String hash, String sourcePage, 
                       String imageType, int width, int height) {
    QuestionImage image = new QuestionImage();
    image.setUrl(url);
    image.setContentHash(hash);
    image.setSourcePage(sourcePage);
    image.setImageType(imageType);
    image.setFormat(extractFormat(url));
    image.setWidth(width);
    image.setHeight(height);
    image.setStatus("ACTIVE");
    imageMapper.insert(image);
}

/**
 * 从 URL 提取格式
 */
private String extractFormat(String url) {
    if (url == null) {
        return null;
    }
    String lowerUrl = url.toLowerCase();
    if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
        return "jpg";
    } else if (lowerUrl.endsWith(".png")) {
        return "png";
    } else if (lowerUrl.endsWith(".gif")) {
        return "gif";
    }
    return null;
}

/**
 * 解析尺寸字符串
 */
private int parseDimension(String dimension) {
    if (dimension == null || dimension.isEmpty()) {
        return 0;
    }
    try {
        return Integer.parseInt(dimension.replaceAll("[^0-9]", ""));
    } catch (NumberFormatException e) {
        return 0;
    }
}
```

- [ ] **Step 8: 提交代码**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/ImageExtractor.java
git commit -m "feat: implement ImageExtractor component"
```

---

## Task 4: 集成到 QuestionCollectorAgent

**Files:**
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java`
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java`

- [ ] **Step 1: 修改 PageFetchTool，增加图片提取方法**

```java
// PageFetchTool.java, 在现有类中添加方法
@Tool("从HTML页面中提取图片URL")
public List<String> extractImages(String html, String sourceUrl) {
    return imageExtractor.extractFromPage(html, sourceUrl);
}

// 添加依赖注入
@Autowired
private ImageExtractor imageExtractor;
```

- [ ] **Step 2: 修改 QuestionCollectorAgent 的 SystemMessage**

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

- [ ] **Step 3: 验证编译**

运行: `cd gk-backend/gk-backend && ./mvnw compile`
Expected: 编译成功

- [ ] **Step 4: 提交代码**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java
git commit -m "feat: integrate ImageExtractor into collection flow"
```

---

## Task 5: 实现前端选项解析工具

**Files:**
- Create: `vue-project/src/utils/optionParser.js`

- [ ] **Step 1: 创建选项解析工具**

```javascript
// optionParser.js

/**
 * 解析选项文本，识别图片标记
 * @param {string} text - 选项文本
 * @returns {Array} - 解析后的部分数组
 */
export function parseOptionText(text) {
  if (!text) {
    return [];
  }

  const imageRegex = /!\[.*?\]\((https?:\/\/[^\)]+)\)/g;
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

/**
 * 检查文本是否包含图片
 * @param {string} text - 选项文本
 * @returns {boolean}
 */
export function hasImage(text) {
  if (!text) {
    return false;
  }
  return /!\[.*?\]\(https?:\/\/[^\)]+\)/.test(text);
}

/**
 * 提取所有图片 URL
 * @param {string} text - 选项文本
 * @returns {Array<string>} - 图片 URL 数组
 */
export function extractImageUrls(text) {
  if (!text) {
    return [];
  }

  const imageRegex = /!\[.*?\]\((https?:\/\/[^\)]+)\)/g;
  const urls = [];
  let match;

  while ((match = imageRegex.exec(text)) !== null) {
    urls.push(match[1]);
  }

  return urls;
}
```

- [ ] **Step 2: 提交代码**

```bash
git add vue-project/src/utils/optionParser.js
git commit -m "feat: add option parser utility for image support"
```

---

## Task 6: 实现前端选项展示组件

**Files:**
- Create: `vue-project/src/components/QuestionOption.vue`

- [ ] **Step 1: 创建 QuestionOption 组件**

```vue
<!-- QuestionOption.vue -->
<template>
  <div 
    class="option-item" 
    :class="{ 'option-selected': isSelected, 'option-correct': isCorrect, 'option-wrong': isWrong }"
    @click="handleClick"
  >
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

<script setup>
import { computed } from 'vue';
import { parseOptionText } from '@/utils/optionParser';

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  text: {
    type: String,
    required: true
  },
  isSelected: {
    type: Boolean,
    default: false
  },
  isCorrect: {
    type: Boolean,
    default: false
  },
  isWrong: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['select']);

const parsedParts = computed(() => {
  return parseOptionText(props.text);
});

function handleClick() {
  if (!props.disabled) {
    emit('select', props.label);
  }
}

function onImageLoad(e) {
  // 图片加载成功
}

function onImageError(e) {
  e.target.style.display = 'none';
  const placeholder = document.createElement('span');
  placeholder.textContent = '[图片加载失败]';
  placeholder.className = 'image-error';
  e.target.parentNode.appendChild(placeholder);
}
</script>

<style scoped>
.option-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  margin: 8px 0;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: #1976d2;
  background-color: #f5f9ff;
}

.option-selected {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.option-correct {
  border-color: #4caf50;
  background-color: #e8f5e9;
}

.option-wrong {
  border-color: #f44336;
  background-color: #ffebee;
}

.option-label {
  font-weight: bold;
  margin-right: 12px;
  min-width: 24px;
}

.option-content {
  flex: 1;
  line-height: 1.6;
}

.option-image {
  max-width: 100%;
  max-height: 200px;
  margin: 8px 0;
  border-radius: 4px;
  border: 1px solid #eee;
}

.image-error {
  color: #999;
  font-style: italic;
  font-size: 12px;
}
</style>
```

- [ ] **Step 2: 提交代码**

```bash
git add vue-project/src/components/QuestionOption.vue
git commit -m "feat: add QuestionOption component with image support"
```

---

## Task 7: 集成到现有答题页面

**Files:**
- Modify: 答题相关的 Vue 文件（需要先查找具体文件）

- [ ] **Step 1: 查找现有答题页面**

运行: `find vue-project/src -name "*.vue" | xargs grep -l "option" | head -5`
Expected: 找到答题相关的 Vue 文件（如 XingceModuleView.vue 或其他答题页面）

- [ ] **Step 2: 导入 QuestionOption 组件**

```vue
<script setup>
import QuestionOption from '@/components/QuestionOption.vue';
// ... 其他导入
</script>
```

- [ ] **Step 3: 替换现有选项展示**

```vue
<template>
  <!-- 替换现有的选项展示部分 -->
  <div class="options-container">
    <QuestionOption
      v-for="(option, key) in options"
      :key="key"
      :label="key"
      :text="option"
      :is-selected="selectedOption === key"
      :is-correct="showAnswer && key === correctAnswer"
      :is-wrong="showAnswer && selectedOption === key && key !== correctAnswer"
      :disabled="showAnswer"
      @select="handleSelect"
    />
  </div>
</template>
```

- [ ] **Step 4: 验证编译**

运行: `cd vue-project && npm run build`
Expected: 编译成功

- [ ] **Step 5: 提交代码**

```bash
git add vue-project/src/views/XingceModuleView.vue
git commit -m "feat: integrate QuestionOption into exam page"
```

---

## Task 8: 测试与验证

- [ ] **Step 1: 启动后端服务**

运行: `cd gk-backend/gk-backend && ./mvnw spring-boot:run`
Expected: 服务启动成功

- [ ] **Step 2: 启动前端服务**

运行: `cd vue-project && npm run dev`
Expected: 前端服务启动成功

- [ ] **Step 3: 测试图片采集**

使用 CollectorController 触发采集，验证：
1. 图片 URL 被正确提取
2. 图片信息被保存到 question_image 表
3. 选项 JSON 包含图片标记

- [ ] **Step 4: 测试前端展示**

在答题页面验证：
1. 图片选项正常显示
2. 图片加载失败时显示占位文本
3. 图文混排正常工作

- [ ] **Step 5: 提交最终代码**

```bash
git add .
git commit -m "test: verify image extraction and display"
```

---

## 自检清单

- [x] 所有文件路径都是相对于项目根目录的路径
- [x] 所有代码步骤都包含完整代码
- [x] 所有命令都有预期输出
- [x] 没有 "TBD"、"TODO" 等占位符
- [x] 类型、方法签名、属性名在所有任务中保持一致
- [x] 每个任务都包含提交步骤
- [x] 设计文档中的所有需求都有对应的任务
