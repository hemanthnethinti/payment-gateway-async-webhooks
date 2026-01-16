package com.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

import java.net.URI;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${REDIS_URL:redis://redis:6379}")
    private String redisUrl;

    @Bean
    public JedisPool jedisPool() {
        try {
            URI uri = URI.create(redisUrl);
            // JedisPool(URI) supports redis:// and rediss:// schemes
            logger.info("Initializing JedisPool with URL: {}", redisUrl);
            return new JedisPool(uri);
        } catch (Exception e) {
            logger.error("Failed to initialize JedisPool from URL '{}', defaulting to localhost:6379", redisUrl, e);
            return new JedisPool("localhost", 6379);
        }
    }
}
