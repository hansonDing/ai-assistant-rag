package com.example.aiassistant.service;

import com.example.aiassistant.config.ConfluenceProperties;
import com.example.aiassistant.model.KnowledgeDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceServiceImpl implements ConfluenceService {

    private final ConfluenceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<KnowledgeDocument> searchPages(String query) {
        return searchPages(query, properties.getMaxResults());
    }

    @Override
    public List<KnowledgeDocument> searchPages(String query, int limit) {
        if (!isEnabled()) {
            log.warn("Confluence integration is not enabled or not properly configured");
            return new ArrayList<>();
        }

        log.info("Searching Confluence for query: '{}' (limit={})", query, limit);
        List<KnowledgeDocument> results = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // Use CQL (Confluence Query Language) to search
            String cql = String.format("text ~ \"%s\"", query.replace("\"", "\\\""));
            if (properties.getSpaceKey() != null && !properties.getSpaceKey().isEmpty()) {
                cql = String.format("space = %s AND %s", properties.getSpaceKey(), cql);
            }

            String url = String.format("%s/rest/api/content/search?cql=%s&limit=%d&expand=body.storage,space",
                    properties.getBaseUrl().replaceAll("/$", ""),
                    java.net.URLEncoder.encode(cql, StandardCharsets.UTF_8),
                    limit);

            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Basic " + getAuthToken());
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode resultsNode = root.path("results");

                    for (JsonNode result : resultsNode) {
                        KnowledgeDocument doc = parseConfluenceResult(result);
                        if (doc != null) {
                            results.add(doc);
                        }
                    }
                    log.info("Found {} pages in Confluence", results.size());
                } else {
                    log.error("Confluence search failed with status {}: {}", statusCode, responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Error searching Confluence: {}", e.getMessage(), e);
        }

        return results;
    }

    @Override
    public KnowledgeDocument getPageContent(String pageId) {
        if (!isEnabled()) {
            return null;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = String.format("%s/rest/api/content/%s?expand=body.storage,space,version",
                    properties.getBaseUrl().replaceAll("/$", ""),
                    pageId);

            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Basic " + getAuthToken());
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode root = objectMapper.readTree(responseBody);
                    return parseConfluenceResult(root);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching Confluence page {}: {}", pageId, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() 
                && properties.getBaseUrl() != null 
                && !properties.getBaseUrl().isEmpty()
                && properties.getUsername() != null
                && !properties.getUsername().isEmpty()
                && properties.getApiToken() != null
                && !properties.getApiToken().isEmpty();
    }

    private String getAuthToken() {
        String auth = properties.getUsername() + ":" + properties.getApiToken();
        return Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    private KnowledgeDocument parseConfluenceResult(JsonNode result) {
        try {
            String id = result.path("id").asText();
            String title = result.path("title").asText();
            String type = result.path("type").asText();
            
            // Get content
            String content = "";
            JsonNode body = result.path("body");
            if (!body.isMissingNode()) {
                JsonNode storage = body.path("storage");
                if (!storage.isMissingNode()) {
                    content = storage.path("value").asText();
                    // Strip HTML tags for plain text
                    content = stripHtml(content);
                }
            }

            // Build URL
            String url = "";
            JsonNode links = result.path("_links");
            if (!links.isMissingNode()) {
                String webui = links.path("webui").asText();
                url = properties.getBaseUrl().replaceAll("/$", "") + webui;
            }

            return KnowledgeDocument.builder()
                    .id("conf_" + id)
                    .title(title)
                    .content(content)
                    .sourceType("CONFLUENCE")
                    .sourceUrl(url)
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing Confluence result: {}", e.getMessage());
            return null;
        }
    }

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // Simple HTML tag removal
        return html
                .replaceAll("\u003c[^\u003e]*\u003e", " ")
                .replaceAll("\u0026\w+;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
