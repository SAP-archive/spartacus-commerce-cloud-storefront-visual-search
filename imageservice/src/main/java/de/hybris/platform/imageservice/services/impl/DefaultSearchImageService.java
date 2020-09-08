package de.hybris.platform.imageservice.services.impl;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import de.hybris.platform.imageservice.cache.model.SearchImageCacheData;
import de.hybris.platform.imageservice.dto.SearchImageData;
import de.hybris.platform.imageservice.exceptions.SearchImageException;
import de.hybris.platform.imageservice.services.SearchImageProviderService;
import de.hybris.platform.imageservice.services.SearchImageService;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link SearchImageService}. Before calling search image provider, the
 * service will first try to get data from cache.
 */
@Service
public class DefaultSearchImageService implements SearchImageService
{
  @Value("${cache.name}")
  private String cacheName;

  @Autowired
  private CacheManager searchImageCacheManager;

  @Autowired
  private SearchImageProviderService searchImageProviderService;

  @Override
  public Mono<SearchImageData> getSearchImageDataFromImage(final ByteBuffer imageBuffer)
  {
    if (imageBuffer == null)
    {
      return Mono.error(new SearchImageException("Item buffer must not be empty"));
    }

    return searchImageProviderService.detectObjects(imageBuffer)
        .filter(detectedObject -> !CollectionUtils.isEmpty(detectedObject.getBoundingBoxes()))
        .map(detectedObject -> {
          detectedObject.getBoundingBoxes()
              .stream()
              .forEach(item -> {
                final String key = UUID.randomUUID()
                    .toString();
                putSearchImageCacheData(key, item.getId());
                item.setId(key);
              });
          return detectedObject;
        });
  }

  @Override
  public Mono<Collection<String>> getSimilarProductIds(final String itemId)
  {
    if (StringUtils.isEmpty(itemId))
    {
      return Mono.error(new SearchImageException("Item id must not be empty"));
    }
    SearchImageCacheData cachedData = getSearchImageCacheData(itemId);
    if (cachedData != null)
    {
      if (!CollectionUtils.isEmpty(cachedData.getIds()))
      {
        return Mono.just(cachedData.getIds());
      } else if (!StringUtils.isEmpty(cachedData.getLink()))
      {
        return searchImageProviderService.getSimilarProducts(cachedData.getLink())
            .map(productIds -> {
              updateSearchImageData(cachedData.getKey(), productIds);
              return productIds;
            });
      }
    }

    return Mono.empty();
  }

  protected SearchImageCacheData getSearchImageCacheData(final String key)
  {
    return searchImageCacheManager.getCache(cacheName)
        .get(key, SearchImageCacheData.class);
  }

  protected void delete(String key)
  {
    searchImageCacheManager.getCache(cacheName)
        .evict(key);
  }

  protected void putSearchImageCacheData(final String key, final String offers)
  {
    if (!StringUtils.isEmpty(offers))
    {
      SearchImageCacheData cachedData = new SearchImageCacheData();
      cachedData.setKey(key);
      cachedData.setLink(offers);
      searchImageCacheManager.getCache(cacheName)
          .put(key, cachedData);
    }
  }

  protected SearchImageCacheData updateSearchImageData(final String key, final List<String> ids)
  {
    SearchImageCacheData cachedData = getSearchImageCacheData(key);
    if (cachedData != null)
    {
      delete(key);
      cachedData.setIds(ids);

      searchImageCacheManager.getCache(cacheName)
          .put(key, cachedData);
    }
    return cachedData;
  }
}
