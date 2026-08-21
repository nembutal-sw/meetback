package com.meetback.dev.transport.client;

import com.meetback.dev.transport.dto.LastTrainDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class OdsaySubwayClient {

    private final RestClient restClient;
    private final String apiKey;

    public OdsaySubwayClient(
            @Value("${odsay.base-url}") String baseUrl,
            @Value("${odsay.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public LastTrainDTO findLastTrain(
            String startStationId,
            String endStationId,
            int day
    ) {

        JsonNode response;

        try {

            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("subwayPathSchedule")
                            .queryParam("SID", startStationId)
                            .queryParam("EID", endStationId)
                            .queryParam("MODE", 4)
                            .queryParam("DAY", day)
                            .queryParam("apiKey", "{apiKey}")
                            .build(apiKey)
                    )
                    .retrieve()
                    .body(JsonNode.class);

        } catch (RestClientResponseException e) {

            throw new IllegalStateException(
                    "ODsay 막차 조회 실패: ["
                            + e.getStatusCode()
                            + "] "
                            + e.getResponseBodyAsString(),
                    e
            );
        }

        if (response == null) {
            throw new IllegalStateException(
                    "ODsay 막차 응답이 없습니다."
            );
        }

        JsonNode error = response.path("error");

        if (!error.isMissingNode()
                && !error.isNull()
                && !error.isEmpty()) {

            throw new IllegalStateException(
                    "ODsay 막차 오류 응답: " + error
            );
        }

        JsonNode paths =
                response.path("result").path("path");

        if (!paths.isArray() || paths.isEmpty()) {
            throw new IllegalStateException(
                    "ODsay 막차 경로가 없습니다."
            );
        }

        // 우선 첫 번째 막차 경로 사용
        JsonNode info =
                paths.get(0).path("info");

        String departureTime =
                info.path("departureTime").asString();

        String arrivalTime =
                info.path("arrivalTime").asString();

        int totalMinutes =
                info.path("totalTime").asInt();

        int transferCount =
                info.path("transferCount").asInt();

        return new LastTrainDTO(
                departureTime,
                arrivalTime,
                totalMinutes,
                transferCount
        );
    }
}
