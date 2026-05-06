package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.QuestionImage;
import com.zwy.gk_backend.mapper.QuestionImageMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageExtractor {

    private static final Logger log = LoggerFactory.getLogger(ImageExtractor.class);

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
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

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

                if (isRelevantImage(src, alt, width, height)) {
                    String contentType = validateAndGetContentType(src);
                    if (contentType != null) {
                        String hash = calculateHash(src);
                        if (!isDuplicate(hash)) {
                            String format = extractFormatFromContentType(contentType);
                            saveImage(src, hash, sourceUrl, "OPTION", format, width, height);
                            imageUrls.add(src);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract images from page: {}", sourceUrl, e);
        }

        return imageUrls;
    }

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
            String contentType = validateAndGetContentType(url);
            if (contentType != null) {
                String hash = calculateHash(url);
                if (!isDuplicate(hash)) {
                    String format = extractFormatFromContentType(contentType);
                    saveImage(url, hash, null, "OPTION", format, 0, 0);
                }
                imageUrls.add(url);
            }
        }

        return imageUrls;
    }

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

    /**
     * 验证图片并返回 Content-Type，失败返回 null
     */
    private String validateAndGetContentType(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            String contentType = connection.getContentType();
            if (contentType == null) {
                return null;
            }

            // 检查是否为图片格式
            if (!contentType.startsWith("image/")) {
                return null;
            }

            // 检查是否为支持的格式
            String format = contentType.substring(6).toLowerCase();
            if (!format.equals("jpeg") && !format.equals("jpg") &&
                !format.equals("png") && !format.equals("gif")) {
                return null;
            }

            return contentType;
        } catch (IOException e) {
            log.debug("Failed to validate image URL: {}", url, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 计算图片内容哈希（下载前 1KB 计算 MD5）
     */
    private String calculateHash(String url) {
        HttpURLConnection connection = null;
        try {
            URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Range", "bytes=0-1023");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            try (InputStream is = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead = is.read(buffer);
                if (bytesRead > 0) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(buffer, 0, bytesRead);
                    byte[] hashBytes = md.digest();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to hash image content, falling back to URL hash: {}", url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Fallback: hash URL string
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
                           String imageType, String format, int width, int height) {
        QuestionImage image = new QuestionImage();
        image.setUrl(url);
        image.setContentHash(hash);
        image.setSourcePage(sourcePage);
        image.setImageType(imageType);
        image.setFormat(format);
        image.setWidth(width);
        image.setHeight(height);
        image.setStatus("ACTIVE");
        imageMapper.insert(image);
    }

    /**
     * 从 Content-Type 提取格式
     */
    private String extractFormatFromContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String format = contentType.substring(6).toLowerCase();
        if (format.equals("jpeg")) {
            return "jpg";
        }
        return format;
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
}
