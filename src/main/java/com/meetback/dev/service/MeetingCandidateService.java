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
import java.math.RoundingMode;
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


        MeetingCandidate existingCandidate =
                meetingCandidateMapper
                        .findByMeetingIdAndParticipantId(
                                participant.getMeetingId(),
                                participantId
                        );


        if (candidateQuery == null
                || candidateQuery.isBlank()) {

            if (existingCandidate == null) {
                return;
            }


            if (Boolean.FALSE.equals(
                    existingCandidate.getIsActive()
            )) {
                return;
            }


            existingCandidate.setIsActive(
                    false
            );


            meetingCandidateMapper.update(
                    existingCandidate
            );


            returnResultMapper.deleteByCandidateId(
                    existingCandidate.getCandidateId()
            );


            return;
        }


        List<PlaceDTO> results =
                kakaoLocalClient.search(
                        candidateQuery.trim()
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



        if (existingCandidate == null) {

            meetingCandidateMapper.insert(
                    candidate
            );

            return;
        }



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



        if (sameCandidate) {


            if (Boolean.FALSE.equals(
                    existingCandidate.getIsActive()
            )) {

                existingCandidate.setIsActive(
                        true
                );


                meetingCandidateMapper.update(
                        existingCandidate
                );
            }


            return;
        }


        candidate.setCandidateId(
                existingCandidate.getCandidateId()
        );


        meetingCandidateMapper.update(
                candidate
        );


        returnResultMapper.deleteByCandidateId(
                existingCandidate.getCandidateId()
        );
    }


    private boolean sameBigDecimal(
            BigDecimal first,
            BigDecimal second
    ) {

        if (first == null
                && second == null) {

            return true;
        }


        if (first == null
                || second == null) {

            return false;
        }


        return first
                .setScale(
                        7,
                        RoundingMode.HALF_UP
                )
                .compareTo(
                        second.setScale(
                                7,
                                RoundingMode.HALF_UP
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