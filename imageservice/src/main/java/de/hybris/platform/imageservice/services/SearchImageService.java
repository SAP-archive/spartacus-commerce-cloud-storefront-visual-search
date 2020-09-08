package de.hybris.platform.imageservice.services;

import java.nio.ByteBuffer;
import java.util.Collection;
import de.hybris.platform.imageservice.dto.SearchImageData;
import reactor.core.publisher.Mono;

public interface SearchImageService
{
  /**
   * Returns {@link SearchImageData} information on items detected on provided image.
   *
   * @param imageBuffer - uploaded image
   * @return bounding boxes and labels for the detected items
   */
  public Mono<SearchImageData> getSearchImageDataFromImage(final ByteBuffer imageBuffer);

  /**
   * Returns list of similar products id for the provided item.
   *
   * @param itemId - id of item on previously uploaded image
   * @return similar products id
   */
  public Mono<Collection<String>> getSimilarProductIds(final String itemId);

}
