package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingCandidateService {

    private final MeetingCandidateMapper meetingCandidateMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;
    private final KakaoLocalClient kakaoLocalClient;


    public void saveCandidate(
            Long participantId,
            String candidateQuery) {
        MeetingParticipant participant =
                meetingParticipantMapper.findById(participantId);
        if (participant == null) {
            throw new IllegalArgumentException(
                    "참가자를 찾을 수 없습니다."
            );
        }
        List<PlaceDTO> results =
                kakaoLocalClient.search(candidateQuery);
        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "희망 장소를 찾을 수 없습니다."
            );
        }
        // 테스트니까 첫 번째 검색 결과 사용
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
                place.roadAddress().isBlank()
                        ? place.address()
                        : place.roadAddress()
        );
        candidate.setLatitude(
                place.latitude()
        );

        candidate.setLongitude(
                place.longitude()
        );

        candidate.setIsActive(
                true
        );
        meetingCandidateMapper.insert(
                candidate
        );
        // 출발 + 귀가 + 희망장소까지 완료
        meetingParticipantMapper.completeInput(
                participantId
        );
    }
    public List<MeetingCandidate> findByMeetingId(Long meetingId) {

        return meetingCandidateMapper.findByMeetingId(meetingId);
    }
}