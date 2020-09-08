package de.hybris.platform.imageservice.cache;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom implementation of cache to enable two level caching.
 */
@Slf4j
public class SearchImageCache implements Cache
{
  private String name;
  private Cache levelOneCache;
  private Cache nextLevelCache;

  SearchImageCache(final String name, final Cache cache)
  {
    this(name, cache, new NoOpCache(name));
  }

  SearchImageCache(final String name, final Cache levelOneCache, final Cache nextLevelCache)
  {
    this.name = name;
    this.levelOneCache = levelOneCache;
    this.nextLevelCache = nextLevelCache;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public Object getNativeCache()
  {
    return this;
  }

  @Override
  public ValueWrapper get(Object o)
  {
    log.debug("get from cache");

    ValueWrapper value = levelOneCache.get(o);
    if (value == null)
    {
      value = nextLevelCache.get(o);
      if (value != null)
      {
        levelOneCache.put(o, value.get());
      }
    }
    return value;
  }

  @Override
  public <T> T get(Object o, Class<T> aClass)
  {
    log.debug("get from cache");

    T value = levelOneCache.get(o, aClass);
    if (value == null)
    {
      value = nextLevelCache.get(o, aClass);
      if (value != null)
      {
        levelOneCache.put(o, value);
      }
    }
    return value;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader)
  {
    log.debug("get from cache");

    T value = levelOneCache.get(key, valueLoader);
    if (value == null)
    {
      value = nextLevelCache.get(key, valueLoader);
      if (value != null)
      {
        levelOneCache.put(key, value);
      }
    }
    return value;
  }

  @Override
  public void put(Object o, Object o1)
  {
    log.debug("put into cache");
    levelOneCache.put(o, o1);
    nextLevelCache.put(o, o1);
  }

  @Override
  public ValueWrapper putIfAbsent(Object o, Object o1)
  {
    // synchronize?
    ValueWrapper value = get(o);
    if (value == null)
    {
      put(o, o1);
    }
    return value;
  }

  @Override
  public void evict(Object o)
  {
    levelOneCache.evict(o);
    nextLevelCache.evict(o);
  }

  @Override
  public void clear()
  {
    levelOneCache.clear();
    nextLevelCache.clear();
  }
}
