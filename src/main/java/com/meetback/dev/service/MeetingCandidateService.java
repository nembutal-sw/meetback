package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MeetingCandidateService {

    private final MeetingCandidateMapper meetingCandidateMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final CandidateReturnResultMapper returnResultMapper;
    private final KakaoLocalClient kakaoLocalClient;


    @Transactional
    public void saveCandidate(
            Long participantId,
            String candidateQuery
    ) {

        MeetingParticipant participant =
                meetingParticipantMapper.findById(
                        participantId
                );

        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }


        if (candidateQuery == null
                || candidateQuery.isBlank()) {

            // 희망 장소는 선택사항
            return;
        }


        List<PlaceDTO> results =
                kakaoLocalClient.search(
                        candidateQuery
                );

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "희망 장소를 찾을 수 없습니다."
            );
        }


        PlaceDTO place =
                results.get(0);


        MeetingCandidate candidate =
                new MeetingCandidate();

        candidate.setMeetingId(
                participant.getMeetingId()
        );

        candidate.setProposerParticipantId(
                participantId
        );

        candidate.setPlaceName(
                place.name()
        );

        candidate.setAddress(
                place.roadAddress() == null
                        || place.roadAddress().isBlank()
                        ? place.address()
                        : place.roadAddress()
        );

        candidate.setLatitude(
                BigDecimal.valueOf(
                        place.latitude()
                )
        );

        candidate.setLongitude(
                BigDecimal.valueOf(
                        place.longitude()
                )
        );

        candidate.setIsActive(
                true
        );


        /*
         * 기존 희망 장소 조회
         */
        MeetingCandidate existingCandidate =
                meetingCandidateMapper
                        .findByMeetingIdAndParticipantId(
                                participant.getMeetingId(),
                                participantId
                        );


        /*
         * 기존 후보가 없으면 새로 등록
         */
        if (existingCandidate == null) {

            meetingCandidateMapper.insert(
                    candidate
            );

            return;
        }


        /*
         * 기존 후보와 새 후보가 실제로 같은지 확인
         */
        boolean sameCandidate =
                Objects.equals(
                        existingCandidate.getPlaceName(),
                        candidate.getPlaceName()
                )
                        && Objects.equals(
                        existingCandidate.getAddress(),
                        candidate.getAddress()
                )
                        && sameBigDecimal(
                        existingCandidate.getLatitude(),
                        candidate.getLatitude()
                )
                        && sameBigDecimal(
                        existingCandidate.getLongitude(),
                        candidate.getLongitude()
                );


        /*
         * 같은 장소면 아무것도 안 함
         */
        if (sameCandidate) {
            return;
        }


        /*
         * 실제 후보 장소가 변경된 경우
         * candidate_id는 유지하고 장소 정보만 수정
         */
        candidate.setCandidateId(
                existingCandidate.getCandidateId()
        );


        meetingCandidateMapper.update(
                candidate
        );


        /*
         * 이 후보에 대한 기존 귀가 계산 결과만 삭제
         *
         * 다른 후보의 결과는 그대로 유지해서 재사용
         */
        returnResultMapper.deleteByCandidateId(
                existingCandidate.getCandidateId()
        );
    }


    private boolean sameBigDecimal(
            BigDecimal first,
            BigDecimal second
    ) {

        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        return first.setScale(
                7,
                java.math.RoundingMode.HALF_UP
        ).compareTo(
                second.setScale(
                        7,
                        java.math.RoundingMode.HALF_UP
                )
        ) == 0;
    }


    public List<MeetingCandidate> findByMeetingId(
            Long meetingId
    ) {

        return meetingCandidateMapper
                .findByMeetingId(
                        meetingId
                );
    }
}