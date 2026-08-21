package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingCandidateService {

    private final MeetingCandidateMapper meetingCandidateMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final KakaoLocalClient kakaoLocalClient;


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


        // 검색 결과 중 첫 번째 장소 사용
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
         * 이 참가자가 기존에 등록한 희망 장소가 있는지 확인
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

        } else {

            /*
             * 기존 후보가 있으면
             * 같은 candidate_id로 수정
             */
            candidate.setCandidateId(
                    existingCandidate.getCandidateId()
            );

            meetingCandidateMapper.update(
                    candidate
            );
        }
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