package com.tavern.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
    String path = exchange.getRequest().getPath().subPath(1).value(); // Remove "/tavern" prefix
    HttpHeaders headers = exchange.getRequest().getHeaders();

    // Build target URL
    String targetUrl = tavernApiUrl + path;
    log.info("proxy method:{} targetUrl: {}", method, targetUrl);
    // Forward request using WebClient
    return webClient.method(method)
      .uri(targetUrl)
      .headers(headersToCopy -> headersToCopy.addAll(headers))
      .contentType(MediaType.valueOf(Objects.requireNonNull(headers.getFirst(HttpHeaders.CONTENT_TYPE))))
      .body(exchange.getRequest().getBody(), byte[].class)
      .exchangeToMono(clientResponse -> {
        // Copy response headers to client
        exchange.getResponse().getHeaders().addAll(clientResponse.headers().asHttpHeaders());
        exchange.getResponse().setStatusCode(clientResponse.statusCode());

        // Stream response body directly without buffering (convert byte[] to DataBuffer)
        return exchange.getResponse().writeWith(
          clientResponse.bodyToFlux(byte[].class)
            .map(byteArray -> exchange.getResponse().bufferFactory().wrap(byteArray))
        );
      });
  }
}