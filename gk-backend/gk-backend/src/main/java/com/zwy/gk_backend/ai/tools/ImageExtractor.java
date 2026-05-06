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

    /**
     * 计算图片内容哈希（下载前 1KB 计算 MD5）
     */
    private String calculateHash(String url) {
        try {
            URL imageUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Range", "bytes=0-1023");

            try (java.io.InputStream is = connection.getInputStream()) {
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
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            // Fallback: hash URL string
        }
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
}
