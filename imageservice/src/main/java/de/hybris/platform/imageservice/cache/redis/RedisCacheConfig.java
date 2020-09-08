package de.hybris.platform.imageservice.cache.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import de.hybris.platform.imageservice.cache.model.SearchImageCacheData;

@Configuration
@EnableConfigurationProperties(RedisCacheConfigurationProperties.class)
public class RedisCacheConfig
{
  @Value("${cache.name}")
  private String cacheName;

  private Logger LOG = LoggerFactory.getLogger(RedisCacheConfig.class);

  private static RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds)
  {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(timeoutInSeconds));
  }

  @Bean
  public LettuceConnectionFactory redisConnectionFactory(
      RedisCacheConfigurationProperties properties)
  {
    LOG.info("Redis (/Lettuce) configuration enabled. With cache timeout "
        + properties.getTimeoutSeconds() + " seconds.");

    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setHostName(properties.getHost());
    redisStandaloneConfiguration.setPort(properties.getPort());
    return new LettuceConnectionFactory(redisStandaloneConfiguration);
  }

  @Bean
  public RedisTemplate<String, SearchImageCacheData> redisTemplate(RedisConnectionFactory cf)
  {
    RedisTemplate<String, SearchImageCacheData> redisTemplate =
        new RedisTemplate<String, SearchImageCacheData>();
    redisTemplate.setConnectionFactory(cf);
    return redisTemplate;
  }

  @Bean
  public RedisCacheConfiguration cacheConfiguration(RedisCacheConfigurationProperties properties)
  {
    return createCacheConfiguration(properties.getTimeoutSeconds());
  }

  @Bean("redisCacheManager")
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
      RedisCacheConfigurationProperties properties)
  {
    if (!isRedisAvailable(redisConnectionFactory))
    {
      return new NoOpCacheManager();
    }

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    for (Entry<String, Long> cacheNameAndTimeout : properties.getCacheExpirations().entrySet())
    {
      cacheConfigurations.put(cacheNameAndTimeout.getKey(),
          createCacheConfiguration(cacheNameAndTimeout.getValue()));
    }

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(cacheConfiguration(properties))
        .withCacheConfiguration(cacheName, cacheConfiguration(properties)).build();
  }

  protected boolean isRedisAvailable(RedisConnectionFactory redisConnectionFactory)
  {
    try
    {
      redisConnectionFactory.getConnection().ping();
      return true;
    } catch (Exception e)
    {
      return false;
    }
  }
}
