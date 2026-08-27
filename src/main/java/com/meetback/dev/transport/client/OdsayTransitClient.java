package com.meetback.dev.transport.client;

import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.transport.dto.RouteLineDTO;
import com.meetback.dev.transport.dto.RouteMapDTO;
import com.meetback.dev.transport.dto.RoutePointDTO;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.time.Duration;


import java.util.ArrayList;
import java.util.List;

@Component
public class OdsayTransitClient {

    private final RestClient restClient;
    private final String apiKey;


    public OdsayTransitClient(
            @Value("${odsay.base-url}") String baseUrl,
            @Value("${odsay.api-key}") String apiKey
    ) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));


        this.restClient =
                RestClient.builder()
                        .requestFactory(
                                requestFactory
                        )
                        .baseUrl(
                                baseUrl
                        )
                        .build();


        this.apiKey =
                apiKey;
    }


    public TransitRouteDTO findSubwayRoute(
            PlaceDTO start,
            PlaceDTO end
    ) {

        JsonNode response;


        try {

            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("searchPubTransPathT")
                            .queryParam(
                                    "SX",
                                    start.longitude()
                            )
                            .queryParam(
                                    "SY",
                                    start.latitude()
                            )
                            .queryParam(
                                    "EX",
                                    end.longitude()
                            )
                            .queryParam(
                                    "EY",
                                    end.latitude()
                            )
                            .queryParam(
                                    "OPT",
                                    0
                            )
                            .queryParam(
                                    "SearchType",
                                    0
                            )
                            .queryParam(
                                    "SearchPathType",
                                    1
                            )
                            .queryParam(
                                    "apiKey",
                                    "{apiKey}"
                            )
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
        }catch (RestClientException e) {

            throw new IllegalStateException(
                    "ODsay 경로 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",
                    e
            );
        }


        if (response == null) {

            throw new IllegalStateException(
                    "ODsay 응답이 없습니다."
            );
        }


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


        JsonNode path =
                paths.get(0);


        int totalMinutes =
                path.path("info")
                        .path("totalTime")
                        .asInt();


        JsonNode mapObjNode =
                path.path("info")
                        .path("mapObj");


        String mapObj =
                mapObjNode.isMissingNode()
                        || mapObjNode.isNull()
                        ? null
                        : mapObjNode.asString();


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
                    "[ODsay subPath] trafficType="
                            + trafficType
                            + ", startName="
                            + subPath.path("startName").asString()
                            + ", endName="
                            + subPath.path("endName").asString()
                            + ", startID="
                            + subPath.path("startID").asString()
                            + ", endID="
                            + subPath.path("endID").asString()
            );



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



        if (startStationId == null
                || endStationId == null) {

            throw new IllegalStateException(
                    "현재 서비스는 버스·지하철 기반 귀가 경로만 지원합니다."
            );
        }



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
                summary.toString(),
                mapObj
        );
    }



    public RouteMapDTO loadLane(
            String mapObj
    ) {

        if (mapObj == null
                || mapObj.isBlank()) {

            throw new IllegalArgumentException(
                    "경로 mapObj가 없습니다."
            );
        }


        String mapObject =
                mapObj.startsWith("0:0@")
                        ? mapObj
                        : "0:0@" + mapObj;


        JsonNode response;


        try {

            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("loadLane")
                            .queryParam(
                                    "mapObject",
                                    mapObject
                            )
                            .queryParam(
                                    "apiKey",
                                    "{apiKey}"
                            )
                            .build(apiKey))
                    .retrieve()
                    .body(JsonNode.class);

        } catch (RestClientResponseException e) {

            throw new IllegalStateException(
                    "ODsay 노선 그래픽 조회 실패: ["
                            + e.getStatusCode()
                            + "] "
                            + e.getResponseBodyAsString()
                            + " / mapObject="
                            + mapObject,
                    e
            );
        }


        if (response == null) {

            throw new IllegalStateException(
                    "ODsay 노선 그래픽 응답이 없습니다."
            );
        }

        JsonNode error =
                response.path("error");

        if (!error.isMissingNode()
                && !error.isNull()
                && !error.isEmpty()) {
            throw new IllegalStateException(
                    "ODsay 노선 그래픽 오류 응답: "
                            + error.toString()
            );
        }


        JsonNode lanes =
                response.path("result")
                        .path("lane");


        if (!lanes.isArray()
                || lanes.isEmpty()) {

            throw new IllegalStateException(
                    "ODsay 노선 그래픽 데이터가 없습니다."
            );
        }

        List<RouteLineDTO> lines =
                new ArrayList<>();


        for (JsonNode lane : lanes) {

            int type =
                    lane.path("type")
                            .asInt();


            List<RoutePointDTO> points =
                    new ArrayList<>();


            JsonNode sections =
                    lane.path("section");


            if (!sections.isArray()) {
                continue;
            }


            for (JsonNode section : sections) {

                JsonNode graphPositions =
                        section.path("graphPos");


                if (!graphPositions.isArray()) {
                    continue;
                }


                for (JsonNode graphPos : graphPositions) {

                    double longitude =
                            graphPos.path("x")
                                    .asDouble();


                    double latitude =
                            graphPos.path("y")
                                    .asDouble();


                    points.add(
                            new RoutePointDTO(
                                    longitude,
                                    latitude
                            )
                    );
                }
            }



            if (!points.isEmpty()) {

                lines.add(
                        new RouteLineDTO(
                                type,
                                points
                        )
                );
            }
        }


        if (lines.isEmpty()) {

            throw new IllegalStateException(
                    "ODsay 경로 좌표를 찾을 수 없습니다."
            );
        }


        System.out.println(
                "[ODsay loadLane 완료] mapObj="
                        + mapObj
                        + ", lineCount="
                        + lines.size()
        );


        return new RouteMapDTO(
                lines
        );
    }
}