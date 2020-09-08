package de.hybris.platform.imageservice.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom cache configuration. Embedded EhCache is used for first level caching and Redis for the
 * second level caching. If configuration for Redis is not provided, only the first level cache will
 * be used.
 */
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport
{
  @Autowired
  private CacheManager redisCacheManager;

  @Autowired
  private CacheManager jCacheCacheManager;


  @Override
  @Bean("searchImageCacheManager")
  public CacheManager cacheManager()
  {
    return new SearchImageCacheManager(jCacheCacheManager, redisCacheManager);
  }
}
