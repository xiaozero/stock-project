package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WebSearchClient implements Closeable {

    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final int TIMEOUT_SECONDS = 30;

    public static class SearchResult {
        private String title;
        private String snippet;
        private String url;

        public SearchResult(String title, String snippet, String url) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getSnippet() { return snippet; }
        public String getUrl() { return url; }
        public String toDisplayString() {
            return snippet.isEmpty() ? title : title + " - " + snippet;
        }
    }

    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://duckduckgo.com/html/?q=" + encodedQuery;

            HttpGet request = new HttpGet(url);
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            String html = EntityUtils.toString(httpClient.execute(request).getEntity(), StandardCharsets.UTF_8);

            // 解析 DuckDuckGo HTML 结果
            Pattern resultPattern = Pattern.compile(
                "<a class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL
            );
            Pattern snippetPattern = Pattern.compile(
                "<a class=\"result__snippet\"[^>]*>(.*?)</a>",
                Pattern.DOTALL
            );

            Matcher resultMatcher = resultPattern.matcher(html);
            int count = 0;
            while (resultMatcher.find() && count < maxResults) {
                String resultUrl = resultMatcher.group(1);
                String title = resultMatcher.group(2).replaceAll("<[^>]+>", "").trim();

                // 尝试获取 snippet
                String snippet = "";
                int snippetStart = html.indexOf("<a class=\"result__snippet\"", resultMatcher.end());
                if (snippetStart > resultMatcher.end() && snippetStart < resultMatcher.end() + 2000) {
                    Matcher snippetMatcher = snippetPattern.matcher(html.substring(snippetStart));
                    if (snippetMatcher.find()) {
                        snippet = snippetMatcher.group(1).replaceAll("<[^>]+>", "").trim();
                    }
                }

                if (!title.isEmpty() && !resultUrl.contains("duckduckgo")) {
                    results.add(new SearchResult(title, snippet, resultUrl));
                    count++;
                }
            }

            log.info("Search for '{}' returned {} results", query, results.size());
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
        }
        return results;
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (Exception e) {
            log.warn("Failed to close httpClient", e);
        }
    }
}