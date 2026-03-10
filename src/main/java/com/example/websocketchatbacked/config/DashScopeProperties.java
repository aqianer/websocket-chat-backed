package com.milvus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {
    private String apiKey;
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String model = "text-embedding-v4";
    private FallbackConfig fallback = new FallbackConfig();
    private RetryConfig retry = new RetryConfig();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public FallbackConfig getFallback() {
        return fallback;
    }

    public void setFallback(FallbackConfig fallback) {
        this.fallback = fallback;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public String getFallbackModel() {
        return fallback.getModel();
    }

    public static class FallbackConfig {
        private boolean enabled = false;
        private String model = "text-embedding-v3";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private double multiplier = 2.0;
        private int maxDelayMs = 10000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public int getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(int maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }
    }
}
