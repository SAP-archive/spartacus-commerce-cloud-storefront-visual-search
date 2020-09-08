package de.hybris.platform.imageservice.cache.redis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cache.redis")
public class RedisCacheConfigurationProperties
{
  private long timeoutSeconds = 60;
  private int port = 6379;
  private String host = "localhost";
  private Map<String, Long> cacheExpirations = new HashMap<>();
}
