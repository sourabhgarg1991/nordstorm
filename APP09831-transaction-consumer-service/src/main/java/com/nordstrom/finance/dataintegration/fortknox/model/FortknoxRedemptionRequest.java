package com.nordstrom.finance.dataintegration.fortknox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortknoxRedemptionRequest {
  @JsonProperty("request_id")
  private String requestId;

  private String authority;
  private String token;
}
