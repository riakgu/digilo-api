package com.riakgu.digilo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCredentialResponse {

    private Long orderItemId;
    private String variantName;
    private Integer quantity;
    private List<CredentialItem> credentials;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CredentialItem {
        private Long inventoryId;
        private Map<String, Object> credential;
    }
}