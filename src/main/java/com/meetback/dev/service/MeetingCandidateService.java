package com.meetback.dev.service;

import com.meetback.dev.domain.Meeting;
import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.domain.MeetingParticipant;
import com.meetback.dev.domain.MeetingStatus;
import com.meetback.dev.dto.CandidateRequestDTO;
import com.meetback.dev.repository.CandidateReturnResultMapper;
import com.meetback.dev.repository.MeetingCandidateMapper;
import com.meetback.dev.repository.MeetingMapper;
import com.meetback.dev.repository.MeetingParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.meetback.dev.domain.ParticipantStatus;

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
    private final MeetingMapper meetingMapper;


    @Transactional
    public void saveCandidate(
            Long participantId,
            Long userId,
            CandidateRequestDTO request
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

        if (
                participant.getParticipantStatus()
                        != ParticipantStatus.ACTIVE
        ) {
            throw new AccessDeniedException(
                    "강퇴된 참가자는 희망 장소를 등록할 수 없습니다."
            );
        }

        if (
                !Objects.equals(
                        participant.getUserId(),
                        userId
                )
        ) {

            throw new AccessDeniedException(
                    "본인의 희망 장소만 등록할 수 있습니다."
            );
        }

        Meeting meeting =
                meetingMapper.findById(
                        participant.getMeetingId()
                );


        if (
                meeting == null
                        ||
                        meeting.getStatus()
                                != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "현재 희망 장소를 등록하거나 수정할 수 없습니다."
            );
        }


        MeetingCandidate existingCandidate =
                meetingCandidateMapper
                        .findByMeetingIdAndParticipantId(
                                participant.getMeetingId(),
                                participantId
                        );


        if (
                request == null
                        ||
                        request.name() == null
                        ||
                        request.name().isBlank()
        ) {

            if (existingCandidate == null) {
                return;
            }


            if (
                    Boolean.FALSE.equals(
                            existingCandidate.getIsActive()
                    )
            ) {

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


        if (
                request.latitude() == null
                        ||
                        request.longitude() == null
        ) {

            throw new IllegalArgumentException(
                    "희망 장소 좌표가 올바르지 않습니다."
            );
        }


        MeetingCandidate candidate =
                new MeetingCandidate();


        candidate.setMeetingId(
                participant.getMeetingId()
        );


        candidate.setProposerParticipantId(
                participantId
        );


        candidate.setPlaceName(
                request.name().trim()
        );


        candidate.setAddress(
                request.address()
        );


        candidate.setLatitude(
                BigDecimal.valueOf(
                        request.latitude()
                )
        );


        candidate.setLongitude(
                BigDecimal.valueOf(
                        request.longitude()
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

                        &&

                        Objects.equals(
                                existingCandidate.getAddress(),
                                candidate.getAddress()
                        )

                        &&

                        sameBigDecimal(
                                existingCandidate.getLatitude(),
                                candidate.getLatitude()
                        )

                        &&

                        sameBigDecimal(
                                existingCandidate.getLongitude(),
                                candidate.getLongitude()
                        );


        if (sameCandidate) {

            if (
                    Boolean.FALSE.equals(
                            existingCandidate.getIsActive()
                    )
            ) {

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


        // 장소가 변경됐으므로
        // 기존 계산 결과 삭제
        returnResultMapper.deleteByCandidateId(
                existingCandidate.getCandidateId()
        );
    }


    // =========================================================
    // BigDecimal 좌표 비교
    // =========================================================

    private boolean sameBigDecimal(
            BigDecimal first,
            BigDecimal second
    ) {

        if (
                first == null
                        &&
                        second == null
        ) {

            return true;
        }


        if (
                first == null
                        ||
                        second == null
        ) {

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