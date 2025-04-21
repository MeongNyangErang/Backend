package com.meongnyangerang.meongnyangerang.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  @Value("${ELASTICSEARCH_URI}")
  private String elasticsearchUri;

  @Value("${ELASTICSEARCH_USERNAME}")
  private String username;

  @Value("${ELASTICSEARCH_PASSWORD}")
  private String password;

  @Bean
  public ElasticsearchClient elasticsearchClient() {
    RestClient restClient = RestClient.builder(HttpHost.create(elasticsearchUri))
        .setDefaultHeaders(new Header[]{
            new BasicHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)))
        })
        .build();

    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }
}

