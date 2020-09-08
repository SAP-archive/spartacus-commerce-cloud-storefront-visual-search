package de.hybris.platform.imageservice.services.impl.syte;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import de.hybris.platform.imageservice.dto.SearchImageData;
import de.hybris.platform.imageservice.exceptions.SearchImageException;
import de.hybris.platform.imageservice.services.SearchImageParseService;
import de.hybris.platform.imageservice.services.SearchImageProviderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link SearchImageProviderService} using Syte.io search image provider.
 */
@Service
@Slf4j
public class SyteSearchImageProviderService implements SearchImageProviderService
{
  @Autowired
  private SearchImageParseService searchImageParseService;

  @Autowired
  private SyteSearchImageProviderClient syteSearchImageProviderClient;

  @Override
  public Mono<SearchImageData> detectObjects(final ByteBuffer image)
  {
    SearchImageData imageData = new SearchImageData();

    return Mono.fromCallable(() -> {
      if (image == null)
      {
        throw new SearchImageException("Image buffer may not be null");
      }
      return image;
    })
        .doOnNext(ByteBuffer::flip)
        .flatMap(syteSearchImageProviderClient::getBoundingBoxes)
        .map(searchImageParseService::parseItemsOnImage)
        .map(bb -> {
          imageData.setBoundingBoxes(bb);
          return imageData;
        });
  }

  protected Mono<ByteBuffer> decodeImage(final String imageBin)
  {
    return Mono.fromCallable(() -> {
      try
      {
        return ByteBuffer.wrap(Base64.getDecoder()
            .decode(imageBin.split(",")[1]));
      } catch (IndexOutOfBoundsException e)
      {
        log.error(e.getMessage());
        throw new SearchImageException(e);
      }
    });
  }

  @Override
  public Mono<List<String>> getSimilarProducts(final String itemId)
  {
    return syteSearchImageProviderClient.retrieveSimilarProductsResult(itemId)
        .map(searchImageParseService::parseIds)
        .flatMapMany(Flux::fromIterable)
        .collectList();
  }

}
