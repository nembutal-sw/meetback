package com.meetback.dev.transport.service;

import com.meetback.dev.domain.CandidateReturnResult;
import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import com.meetback.dev.transport.client.OdsayTransitClient;
import com.meetback.dev.transport.dto.RouteMapDTO;
import com.meetback.dev.transport.dto.RouteStepDTO;
import com.meetback.dev.transport.dto.TransitRouteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private static final double WALKING_DISTANCE_METERS = 700.0;

    private final MeetingParticipantMapper meetingParticipantMapper;
    private final MeetingCandidateMapper meetingCandidateMapper;
    private final OdsayTransitClient odsayTransitClient;
    private final MeetingMapper meetingMapper;
    private final CandidateReturnResultMapper returnResultMapper;
    private final ObjectMapper objectMapper;


    public boolean isWithinWalkingDistance(
            Long participantId,
            Long candidateId
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }

        double distance =
                calculateDistanceMeters(
                        candidate.getLatitude().doubleValue(),
                        candidate.getLongitude().doubleValue(),
                        participant.getReturnLatitude().doubleValue(),
                        participant.getReturnLongitude().doubleValue()
                );

        System.out.println(
                "[거리 확인] candidateId="
                        + candidateId
                        + ", participantId="
                        + participantId
                        + ", distance="
                        + Math.round(distance)
                        + "m"
        );

        return distance <= WALKING_DISTANCE_METERS;
    }


    private double calculateDistanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        final double EARTH_RADIUS = 6371000.0;

        double latDistance =
                Math.toRadians(lat2 - lat1);

        double lonDistance =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return EARTH_RADIUS * c;
    }


    public TransitRouteDTO testRoute(
            Long participantId,
            Long candidateId
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }

        PlaceDTO departurePlace =
                new PlaceDTO(
                        null,
                        participant.getDepartureName(),
                        participant.getDepartureAddress(),
                        participant.getDepartureAddress(),
                        participant.getDepartureLongitude().doubleValue(),
                        participant.getDepartureLatitude().doubleValue(),
                        null,
                        null
                );

        PlaceDTO meetingPlace =
                new PlaceDTO(
                        null,
                        candidate.getPlaceName(),
                        candidate.getAddress(),
                        candidate.getAddress(),
                        candidate.getLongitude().doubleValue(),
                        candidate.getLatitude().doubleValue(),
                        null,
                        null
                );

        return odsayTransitClient.findSubwayRoute(
                departurePlace,
                meetingPlace
        );
    }


    public TransitRouteDTO searchReturnRoute(
            Long participantId,
            Long candidateId
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }

        PlaceDTO start =
                new PlaceDTO(
                        null,
                        candidate.getPlaceName(),
                        candidate.getAddress(),
                        candidate.getAddress(),
                        candidate.getLongitude().doubleValue(),
                        candidate.getLatitude().doubleValue(),
                        null,
                        null
                );

        PlaceDTO end =
                new PlaceDTO(
                        null,
                        participant.getReturnName(),
                        participant.getReturnAddress(),
                        participant.getReturnAddress(),
                        participant.getReturnLongitude().doubleValue(),
                        participant.getReturnLatitude().doubleValue(),
                        null,
                        null
                );

        return odsayTransitClient.findSubwayRoute(
                start,
                end
        );
    }


    public RouteMapDTO getRouteMap(
            Long candidateId,
            Long participantId
    ) {

        MeetingCandidate candidate =
                meetingCandidateMapper.findById(candidateId);

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "후보 장소를 찾을 수 없습니다."
            );
        }

        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }

        if (!candidate.getMeetingId()
                .equals(participant.getMeetingId())) {

            throw new IllegalArgumentException(
                    "후보 장소와 참가자의 모임이 일치하지 않습니다."
            );
        }

        Meeting meeting =
                meetingMapper.findById(
                        candidate.getMeetingId()
                );

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }

        Integer calculationVersion =
                meeting.getCalculationVersion();

        if (calculationVersion == null) {
            calculationVersion = 0;
        }

        CandidateReturnResult result =
                returnResultMapper
                        .findByCandidateAndParticipantAndVersion(
                                candidateId,
                                participantId,
                                calculationVersion
                        );

        if (result == null) {
            throw new IllegalStateException(
                    "해당 참가자의 귀가 계산 결과가 없습니다."
            );
        }


        /*
         * 일반 경로 계산 시 미리 저장해둔
         * 역 / 호선 정보
         */
        List<RouteStepDTO> steps =
                List.of();


        /*
         * route_map_data가 존재하는 경우
         *
         * 1. lines가 있으면
         *    → 지도 좌표까지 저장된 완성 캐시
         *    → ODsay 재호출 없이 그대로 사용
         *
         * 2. lines가 비어있으면
         *    → 호선/역 정보만 저장된 상태
         *    → steps만 꺼내고 loadLane 진행
         */
        if (result.getRouteMapData() != null
                && !result.getRouteMapData().isBlank()) {

            try {

                RouteMapDTO savedRouteMap =
                        objectMapper.readValue(
                                result.getRouteMapData(),
                                RouteMapDTO.class
                        );


                if (savedRouteMap.steps() != null) {

                    steps =
                            savedRouteMap.steps();
                }


                if (savedRouteMap.lines() != null
                        && !savedRouteMap.lines().isEmpty()) {

                    System.out.println(
                            "[기존 지도 경로 재사용] candidateId="
                                    + candidateId
                                    + ", participantId="
                                    + participantId
                    );

                    return savedRouteMap;
                }


                System.out.println(
                        "[경로 안내 정보 재사용] candidateId="
                                + candidateId
                                + ", participantId="
                                + participantId
                );

            } catch (JacksonException e) {

                throw new IllegalStateException(
                        "저장된 지도 경로 데이터를 읽을 수 없습니다.",
                        e
                );
            }
        }


        /*
         * 지도 좌표가 아직 저장되지 않았다면
         * routeMapObj를 이용해 loadLane 최초 1회 호출
         */
        if (result.getRouteMapObj() == null
                || result.getRouteMapObj().isBlank()) {

            throw new IllegalStateException(
                    "해당 귀가 경로의 지도 정보가 없습니다."
            );
        }


        RouteMapDTO routeMap =
                odsayTransitClient.loadLane(
                        result.getRouteMapObj()
                );


        /*
         * loadLane 지도 좌표
         * +
         * 일반 경로 조회에서 저장해둔
         * 역 / 호선 안내 정보 결합
         */
        RouteMapDTO response =
                new RouteMapDTO(
                        candidate.getPlaceName(),
                        participant.getReturnName(),
                        routeMap.lines(),
                        steps
                );


        /*
         * 완성된 지도 경로를 다시 JSON으로 저장
         *
         * 이후 같은 후보 + 참가자 조회부터는
         * loadLane 호출 없이 이 값을 그대로 사용
         */
        try {

            String routeMapData =
                    objectMapper.writeValueAsString(
                            response
                    );

            returnResultMapper.updateRouteMapData(
                    result.getResultId(),
                    routeMapData
            );

            System.out.println(
                    "[지도 경로 저장] candidateId="
                            + candidateId
                            + ", participantId="
                            + participantId
            );

        } catch (JacksonException e) {

            throw new IllegalStateException(
                    "지도 경로 데이터를 JSON으로 변환할 수 없습니다.",
                    e
            );
        }


        return response;
    }
}