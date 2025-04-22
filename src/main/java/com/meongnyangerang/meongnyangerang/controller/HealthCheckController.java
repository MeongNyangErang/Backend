package com.meongnyangerang.meongnyangerang.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

  @GetMapping("/health")
  @ResponseStatus(HttpStatus.OK)
  public void healthCheck() {
  }
}
