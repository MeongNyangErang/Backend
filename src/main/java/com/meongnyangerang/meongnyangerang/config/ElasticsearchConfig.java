package com.meongnyangerang.meongnyangerang.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  @Bean
  public ElasticsearchClient elasticsearchClient(RestClient restClient) {
    ElasticsearchTransport transport = new RestClientTransport(restClient,
        new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }
}

