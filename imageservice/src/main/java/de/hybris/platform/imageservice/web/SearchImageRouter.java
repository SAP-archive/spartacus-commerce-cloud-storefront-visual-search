package de.hybris.platform.imageservice.web;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SearchImageRouter
{
  @Value("${cors.allowed.origins}")
  private String allowedOrigins;

  @Bean
  public RouterFunction<ServerResponse> route(SearchImageHandler handler)
  {
    return nest(path("/imageservice"), RouterFunctions.route(GET("/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::getSimilarProducts)
        .andRoute(POST("/upload").and(accept(MediaType.MULTIPART_FORM_DATA)), handler::uploadFile));
  }

  @Bean
  CorsWebFilter corsWebFilter()
  {
    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
    corsConfig.setMaxAge(8000L);
    corsConfig.addAllowedMethod("POST");
    corsConfig.addAllowedMethod("GET");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }
}
