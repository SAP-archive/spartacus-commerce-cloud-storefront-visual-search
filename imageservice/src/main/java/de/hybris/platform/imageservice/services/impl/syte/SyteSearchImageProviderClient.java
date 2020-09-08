package de.hybris.platform.imageservice.services.impl.syte;

import java.net.URI;
import java.nio.ByteBuffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import de.hybris.platform.imageservice.exceptions.SearchImageException;
import reactor.core.publisher.Mono;

/**
 * Web client for sending and retrieving data from Syte.io.
 */
@Service
public class SyteSearchImageProviderClient
{

  private static final String ACCOUNT_ID = "account_id";
  private static final String SIGNATURE = "sig";
  private static final String PAYLOAD_TYPE = "payload_type";
  private static final String IMAGE_BIN = "image_bin";

  @Value("${syte.url}")
  private String host;

  @Value("${syte.accountid}")
  private String accountId;

  @Value("${syte.signature}")
  private String signature;

  private WebClient webClient;

  public Mono<String> getBoundingBoxes(MultipartFile file)
  {
    return Mono.fromCallable(() -> webClient().post()
        .uri(uriBuilder -> uriBuilder.queryParam(ACCOUNT_ID, accountId)
            .queryParam(SIGNATURE, signature)
            .queryParam(PAYLOAD_TYPE, IMAGE_BIN)
            .build())
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue(file.getBytes())
        .retrieve())
        .flatMap(rs -> rs.bodyToMono(String.class));
  }

  public Mono<String> getBoundingBoxes(ByteBuffer buffer)
  {
    return Mono.fromCallable(() -> webClient().post()
        .uri(uriBuilder -> uriBuilder.queryParam(ACCOUNT_ID, accountId)
            .queryParam(SIGNATURE, signature)
            .queryParam(PAYLOAD_TYPE, IMAGE_BIN)
            .build())
        .contentType(MediaType.TEXT_PLAIN)
        .bodyValue(buffer.array())
        .retrieve())
        .flatMap(rs -> rs.bodyToMono(String.class));
  }

  public Mono<String> retrieveSimilarProductsResult(final String itemId)
  {
    return Mono.fromCallable(() -> new URI(itemId))
        .map(uri -> WebClient.builder()
            .baseUrl(uri.getScheme() + "://" + uri.getHost())
            .build()
            .get()
            .uri(uriBuilder -> uriBuilder.path(uri.getPath())
                .query(uri.getQuery())
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve())
        .flatMap(responseSpec -> responseSpec.bodyToMono(String.class))
        .onErrorResume(e -> Mono.error(() -> new SearchImageException(e)));
  }

  private WebClient webClient()
  {
    if (webClient == null)
    {
      webClient = WebClient.builder()
          .baseUrl(host)
          .build();
    }
    return webClient;
  }

}
