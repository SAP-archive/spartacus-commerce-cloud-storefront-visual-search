package de.hybris.platform.imageservice.cache.ehcache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EhCacheConfig
{
  @Value("${cache.ehcache.idle.time}")
  private int idleTime;

  @Value("${cache.ehcache.heap.entries.size}")
  private int maxEntries;

  @Value("${cache.ehcache.offheap.mb}")
  private int offHeap;

  @Value("${cache.name}")
  private String cacheName;

  @Bean
  public JCacheCacheManager jCacheCacheManager()
  {
    JCacheCacheManager jCacheManager = new JCacheCacheManager(ehCacheManager());
    jCacheManager.setAllowNullValues(false);
    return jCacheManager;
  }

  @Bean
  public CacheManager ehCacheManager()
  {
    ResourcePools resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
        .heap(maxEntries, EntryUnit.ENTRIES).offheap(offHeap, MemoryUnit.MB).build();


    CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(Object.class, Object.class, resourcePools)
        .withExpiry(ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofSeconds(idleTime))).build();

    Map<String, CacheConfiguration<?, ?>> caches = new HashMap<>();
    caches.put(cacheName, cacheConfiguration);

    EhcacheCachingProvider provider = (EhcacheCachingProvider) Caching
        .getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
    org.ehcache.config.Configuration configuration =
        new DefaultConfiguration(caches, provider.getDefaultClassLoader());

    return provider.getCacheManager(provider.getDefaultURI(), configuration);
  }

}
