package com.meetback.dev.place.client;

import com.meetback.dev.place.dto.PlaceDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class KakaoLocalClient {

    private final RestClient restClient;

    public KakaoLocalClient(
            @Value("${kakao.rest-api-key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(
                        "Authorization",
                        "KakaoAK " + apiKey
                )
                .build();
    }

    public List<PlaceDTO> search(String query) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        List<PlaceDTO> places = new ArrayList<>();

        if (response == null) {
            return places;
        }
        JsonNode documents =
                response.get("documents");
        if (documents == null) {
            return places;
        }
        for (JsonNode document : documents) {
            PlaceDTO place = new PlaceDTO(
                    document.path("id").asString(),
                    document.path("place_name").asString(),
                    document.path("address_name").asString(),
                    document.path("road_address_name").asString(),
                    Double.parseDouble(
                            document.path("x").asString()
                    ),
                    Double.parseDouble(
                            document.path("y").asString()
                    ),
                    document.path("category_name").asString(),
                    document.path("place_url").asString()
            );
            places.add(place);
        }
        return places;
    }
}