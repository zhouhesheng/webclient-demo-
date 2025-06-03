package com.arrayindex.webclientdemo.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/tavern")
public class ProxyController {
  @Value("${tavern.api.url}")
  private String tavernApiUrl;

  private final WebClient webClient;

  public ProxyController(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  @RequestMapping("/**")
  public Mono<Void> proxyRequest(ServerWebExchange exchange) {
    // Get original request details
    HttpMethod method = exchange.getRequest().getMethod();
    URI originalUri = exchange.getRequest().getURI();  // Get full URI with query parameters

    // Extract path (remove "/tavern" prefix) and query
    String path = originalUri.getPath().substring("/tavern".length());  // Remove controller base path
    String query = originalUri.getQuery();

    HttpHeaders headers = exchange.getRequest().getHeaders();

    // Build target URL with path and query
    String targetUrl = tavernApiUrl + path + (query != null ? "?" + query : "");  // Add query if present
    log.info("proxy path:{} method:{} targetUrl: {}", exchange.getRequest().getPath(), method, targetUrl);
    // Forward request using WebClient
    return webClient.method(method)
      .uri(targetUrl)
      .headers(headersToCopy -> headersToCopy.addAll(headers))
      .contentType(MediaType.valueOf(Objects.requireNonNull(headers.getFirst(HttpHeaders.CONTENT_TYPE))))
      .body(exchange.getRequest().getBody(), DataBuffer.class) // Use DataBuffer instead of byte[]
      .exchangeToMono(clientResponse -> {
        // Copy response headers to client
        exchange.getResponse().getHeaders().addAll(clientResponse.headers().asHttpHeaders());
        exchange.getResponse().setStatusCode(clientResponse.statusCode());

        // Stream response body directly without converting to byte[]
        return exchange.getResponse().writeWith(clientResponse.bodyToFlux(DataBuffer.class));
      });
  }
}