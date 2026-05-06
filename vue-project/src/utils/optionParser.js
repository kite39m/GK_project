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
