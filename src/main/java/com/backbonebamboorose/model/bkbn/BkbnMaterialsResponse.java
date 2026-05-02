package com.backbonebamboorose.model.bkbn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BkbnMaterialsResponse {

    @JsonProperty("materials")
    private List<Material> materials;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("assignmentId")
    private String assignmentId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Material {

        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("url")
        private String url;

        @JsonProperty("type")
        private String type;

        @JsonProperty("size")
        private Long size;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("createdAt")
        private String createdAt;
    }
}
