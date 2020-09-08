package de.hybris.platform.imageservice.web;

import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import de.hybris.platform.imageservice.exceptions.SearchImageException;
import de.hybris.platform.imageservice.services.SearchImageService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SearchImageHandler
{
  @Autowired
  private SearchImageService searchImageService;

  public Mono<ServerResponse> uploadFile(ServerRequest request)
  {
    return request.body(BodyExtractors.toMultipartData())
        .flatMap(map -> {
          Map<String, Part> parts = map.toSingleValueMap();
          log.info("file: {}", parts.get("file"));
          return Mono.just((FilePart) parts.get("file"));
        })
        .flatMap(this::getBuffer)
        .flatMap(t -> searchImageService.getSearchImageDataFromImage(t))
        .onErrorResume(e -> Mono.error(() -> new SearchImageException(e)))
        .flatMapMany(sid -> ServerResponse.ok()
            .bodyValue(sid))
        .next();
  }

  public Mono<ServerResponse> getSimilarProducts(ServerRequest request)
  {
    return searchImageService.getSimilarProductIds(request.pathVariable("id"))
        .onErrorResume(e -> Mono.error(() -> new SearchImageException(e)))
        .flatMapMany(sid -> ServerResponse.ok()
            .bodyValue(sid))
        .next();
  }

  private Mono<ByteBuffer> getBuffer(final FilePart filePart)
  {

    final ByteBuffer byteBuffer = ByteBuffer.allocate(8 * 1024 * 1024);

    filePart.content()
        .subscribe(new Subscriber<DataBuffer>()
        {
          @Override
          public void onSubscribe(Subscription s)
          {
            s.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(DataBuffer buffer)
          {
            byteBuffer.put(buffer.asByteBuffer());
          }

          @Override
          public void onError(Throwable t)
          {
            log.info("onError bytebuffer");
          }

          @Override
          public void onComplete()
          {
            log.info("onComplete bytebuffer");
          }
        });

    return Mono.just(byteBuffer);
  }
}
