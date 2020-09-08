package de.hybris.platform.imageservice.cache;

import java.util.Collection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;

/**
 * Implementation of {@link CacheManager} that supports two level caching.
 */
public class SearchImageCacheManager implements CacheManager
{
  private CacheManager firstLevel;
  private CacheManager secondLevel;

  SearchImageCacheManager(final CacheManager singleLevel)
  {
    this(singleLevel, new NoOpCacheManager());
  }

  SearchImageCacheManager(final CacheManager firstLevel, final CacheManager secondLevel)
  {
    this.firstLevel = firstLevel;
    this.secondLevel = secondLevel;
  }

  SearchImageCacheManager(final String name, final CacheManager currentLevel,
      final CacheManager secondLevel)
  {
    this.firstLevel = currentLevel;
    this.secondLevel = secondLevel;
  }

  @Override
  public Cache getCache(String name)
  {
    return new SearchImageCache(name, firstLevel.getCache(name), secondLevel.getCache(name));
  }

  @Override
  public Collection<String> getCacheNames()
  {
    return firstLevel.getCacheNames();
  }

  protected CacheManager getFirstLevelCacheManager()
  {
    return firstLevel;
  }

  protected CacheManager getSecondLevelCacheManager()
  {
    return secondLevel;
  }
}
