package com.meetback.dev.transport.client;

import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

@Component
public class OdsayTransitClient {

    private final RestClient restClient;
    private final String apiKey;

    public OdsayTransitClient(
            @Value("${odsay.base-url}") String baseUrl,
            @Value("${odsay.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }


    public TransitRouteDTO findSubwayRoute(
            PlaceDTO start,
            PlaceDTO end) {

        JsonNode response;

        try {

            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("searchPubTransPathT")
                            .queryParam("SX", start.longitude())
                            .queryParam("SY", start.latitude())
                            .queryParam("EX", end.longitude())
                            .queryParam("EY", end.latitude())
                            .queryParam("OPT", 0)
                            .queryParam("SearchType", 0)
                            .queryParam("SearchPathType", 1)
                            .queryParam("apiKey", "{apiKey}")
                            .build(apiKey))
                    .retrieve()
                    .body(JsonNode.class);

        } catch (RestClientResponseException e) {

            throw new IllegalStateException(
                    "ODsay 경로 조회 실패: ["
                            + e.getStatusCode()
                            + "] "
                            + e.getResponseBodyAsString()
                            + " / 좌표: "
                            + start.longitude()
                            + ","
                            + start.latitude()
                            + " → "
                            + end.longitude()
                            + ","
                            + end.latitude(),
                    e
            );
        }


        if (response == null) {
            throw new IllegalStateException(
                    "ODsay 응답이 없습니다."
            );
        }


        /*
         * ODsay가 HTTP 200으로 오류 JSON을 줄 경우도 확인
         */
        JsonNode error =
                response.path("error");

        if (!error.isMissingNode()
                && !error.isNull()
                && !error.isEmpty()) {

            throw new IllegalStateException(
                    "ODsay 오류 응답: "
                            + error.toString()
            );
        }


        JsonNode result =
                response.path("result");

        JsonNode paths =
                result.path("path");


        if (!paths.isArray()
                || paths.isEmpty()) {

            throw new IllegalStateException(
                    "ODsay 경로 검색 결과가 없습니다. 응답: "
                            + response.toString()
            );
        }


        /*
         * 우선 첫 번째 추천 경로 사용
         */
        JsonNode path =
                paths.get(0);


        int totalMinutes =
                path.path("info")
                        .path("totalTime")
                        .asInt();


        JsonNode subPaths =
                path.path("subPath");


        int subwaySectionCount = 0;

        String startStationId = null;
        String endStationId = null;

        String startStationName = null;
        String endStationName = null;

        StringBuilder summary =
                new StringBuilder();


        for (JsonNode subPath : subPaths) {

            int trafficType =
                    subPath.path("trafficType")
                            .asInt();

            System.out.println(
                    "[ODsay subPath] trafficType=" + trafficType
                            + ", startName=" + subPath.path("startName").asString()
                            + ", endName=" + subPath.path("endName").asString()
                            + ", startID=" + subPath.path("startID").asString()
                            + ", endID=" + subPath.path("endID").asString()
            );

            /*
             * trafficType
             * 1 = 지하철
             */
            if (trafficType == 1) {

                if (startStationId == null) {

                    startStationId =
                            subPath.path("startID")
                                    .asString();

                    startStationName =
                            subPath.path("startName")
                                    .asString();
                }


                endStationId =
                        subPath.path("endID")
                                .asString();

                endStationName =
                        subPath.path("endName")
                                .asString();


                JsonNode lane =
                        subPath.path("lane");


                if (lane.isArray()
                        && !lane.isEmpty()) {

                    String lineName =
                            lane.get(0)
                                    .path("name")
                                    .asString();


                    if (!summary.isEmpty()) {
                        summary.append(" → ");
                    }


                    summary.append(
                            lineName
                    );
                }


                subwaySectionCount++;
            }
        }

        /*
         * 지하철 구간이 없으면
         * 철도/고속버스 등 현재 MVP 미지원 경로
         */
        if (startStationId == null || endStationId == null) {
            throw new IllegalStateException(
                    "현재 서비스는 버스·지하철 기반 귀가 경로만 지원합니다."
            );
        }


        /*
         * 지하철 구간이
         * 1개 → 환승 0회
         * 2개 → 환승 1회
         * 3개 → 환승 2회
         */
        int transferCount =
                Math.max(
                        subwaySectionCount - 1,
                        0
                );


        return new TransitRouteDTO(
                totalMinutes,
                transferCount,
                startStationId,
                endStationId,
                startStationName,
                endStationName,
                summary.toString()
        );
    }
}
