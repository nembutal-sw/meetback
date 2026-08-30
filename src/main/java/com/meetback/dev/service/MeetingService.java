package com.meetback.dev.service;

import com.meetback.dev.domain.*;
import com.meetback.dev.dto.*;
import com.meetback.dev.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingMapper meetingMapper;
    private final ParticipantMapper participantMapper;
    private final CandidateMapper candidateMapper;
    private final ParticipantService participantService;
    private final CandidateService candidateService;
    private final VoteMapper voteMapper;

    private final MeetingCandidateMapper meetingCandidateMapper;
    private final MeetingParticipantMapper meetingParticipantMapper;

    @Transactional
    public MeetingCreateResponse createMeeting(
            Long hostUserId,
            MeetingCreateRequest request
    ) {

        LocalDateTime desiredEndAt =
                request.getDesiredEndAt();


        if (desiredEndAt == null) {

            throw new IllegalArgumentException(
                    "희망 종료시간은 필수입니다."
            );
        }


        if (!desiredEndAt.isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "희망 종료시간은 현재 시각 이후로 선택해주세요."
            );
        }


        /*
         * 현재 기존 방 생성 화면에서는 meetingType을 보내지 않을 수 있으므로
         * 값이 없으면 FRIEND로 생성.
         *
         * 추후 QUICK_VOTE 방 생성 시에는
         * request에서 QUICK_VOTE가 전달됨.
         */
        MeetingType meetingType =
                request.getMeetingType() != null
                        ? request.getMeetingType()
                        : MeetingType.FRIEND;

        LocalDateTime meetingStartAt =
                request.getMeetingStartAt();

        Integer maxParticipants = request.getMaxParticipants();

        /*
         * 고정 번개방 전용 검증
         */
        if (meetingType == MeetingType.QUICK_FIXED)
        {
            /*
             * 1. 고정 장소 필수
             */
            if (
                    request.getFixedPlace() == null
                    ||
                    request.getFixedPlace().name() == null
                    ||
                    request.getFixedPlace().name().isBlank()
                    ||
                    request.getFixedPlace().latitude() == null
                    ||
                    request.getFixedPlace().longitude() == null
            ) {
                throw new IllegalArgumentException(
                        "고정 번개방은 모임 장소가 필수입니다."
                );
            }


            /*
             * 2. 모임 시작시간 필수
             */
            if (meetingStartAt == null) {

                throw new IllegalArgumentException(
                        "고정 번개방은 모임 시작시간이 필수입니다."
                );
            }


            /*
             * 3. 모임 시작시간은 현재 시각 이후
             */
            if (
                    !meetingStartAt.isAfter(
                            LocalDateTime.now()
                    )
            ) {
                throw new IllegalArgumentException(
                        "모임 시작시간은 현재 시각 이후로 선택해주세요."
                );
            }


            /*
             * 4. 종료시간은 시작시간 이후
             */
            if (
                    !desiredEndAt.isAfter(
                            meetingStartAt
                    )
            ) {
                throw new IllegalArgumentException(
                        "희망 종료시간은 모임 시작시간 이후로 선택해주세요."
                );
            }
        }


        /*
         * 투표형 번개방과 고정 번개방 여부
         */
        boolean quickMeeting =
                meetingType == MeetingType.QUICK_VOTE
                ||
                meetingType == MeetingType.QUICK_FIXED;


        /*
         * 두 번개방의 최대 참가 인원 검사
         */
        if (quickMeeting) {

            if (
                    maxParticipants == null
                    ||
                    maxParticipants < 2
                    ||
                    maxParticipants > 100
            ) {
                throw new IllegalArgumentException(
                        "번개방 최대 인원은 2명 이상 100명 이하로 설정해주세요."
                );
            }

        }
        else {

            /*
             * 친구방은 최대 인원 제한을 사용하지 않는다.
             */
            maxParticipants =
                    null;
        }

        /*
         * 5. 방장을 포함한 최대 참가 인원 검사
         */


        String inviteCode = generateInviteCode();

        Meeting meeting = new Meeting();

        meeting.setHostUserId(hostUserId);
        meeting.setTitle(request.getTitle());
        meeting.setMeetingType(meetingType);
        meeting.setMaxParticipants(maxParticipants);
        meeting.setStatus(MeetingStatus.INPUT_OPEN);
        meeting.setDesiredEndAt(request.getDesiredEndAt());
        meeting.setMeetingStartAt(request.getMeetingStartAt());
        meeting.setCalculationVersion(0);
        meeting.setInviteCode(inviteCode);

        meetingMapper.insertMeeting(meeting);



        MeetingParticipant participant =
                new MeetingParticipant();


        participant.setMeetingId(
                meeting.getMeetingId()
        );


        participant.setUserId(
                hostUserId
        );


        participant.setParticipantStatus(
                ParticipantStatus.ACTIVE
        );


        participant.setInputStatus(
                InputStatus.DRAFT
        );


        participantMapper.insertParticipant(
                participant
        );

        if (
                meetingType == MeetingType.QUICK_FIXED
        ) {

            CandidateRequestDTO fixedPlace =
                    request.getFixedPlace();


            MeetingCandidate candidate =
                    new MeetingCandidate();


            candidate.setMeetingId(
                    meeting.getMeetingId()
            );


            candidate.setProposerParticipantId(
                    participant.getParticipantId()
            );


            candidate.setPlaceName(
                    fixedPlace.name().trim()
            );


            candidate.setAddress(
                    fixedPlace.address()
            );


            candidate.setLatitude(
                    BigDecimal.valueOf(
                            fixedPlace.latitude()
                    )
            );


            candidate.setLongitude(
                    BigDecimal.valueOf(
                            fixedPlace.longitude()
                    )
            );


            candidate.setIsActive(
                    true
            );


            meetingCandidateMapper.insert(
                    candidate
            );


            meetingMapper.updateFinalCandidate(
                    meeting.getMeetingId(),
                    candidate.getCandidateId(),
                    MeetingStatus.INPUT_OPEN
            );
        }


        return new MeetingCreateResponse(
                meeting.getMeetingId(),
                inviteCode
        );
    }


    // ============================================================
    // 초대코드 생성
    // ============================================================

    private String generateInviteCode() {

        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }


    // ============================================================
    // 모임 참가
    // ============================================================

    @Transactional
    public MeetingJoinResponse joinMeeting(
            Long userId,
            MeetingJoinRequest request
    ) {

        // 1. 초대코드로 모임 찾기
        Meeting meeting =
                meetingMapper.selectByInviteCode(
                        request.getInviteCode()
                );


        // 2. 존재하지 않는 초대코드
        if (meeting == null) {

            throw new IllegalArgumentException(
                    "유효하지 않은 초대코드입니다."
            );
        }


        // 3. 기존 참가 이력 조회
        MeetingParticipant existingParticipant =
                participantMapper.findAnyByMeetingAndUser(
                        meeting.getMeetingId(),
                        userId
                );


        // 4. 강퇴된 참가자 재입장 차단
        if (
                existingParticipant != null
                        &&
                        existingParticipant.getParticipantStatus()
                                == ParticipantStatus.KICKED
        ) {

            throw new IllegalStateException(
                    "강퇴된 모임에는 다시 참가할 수 없습니다."
            );
        }


        // 5. 이미 정상 참가 중
        if (
                existingParticipant != null
                        &&
                        existingParticipant.getParticipantStatus()
                                == ParticipantStatus.ACTIVE
        ) {

            return new MeetingJoinResponse(
                    meeting.getMeetingId(),
                    false,
                    meeting.getMeetingType()
            );
        }


        /*
         * 신규 참가와 LEFT 재입장은
         * INPUT_OPEN 단계에서만 허용
         */
        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "현재 새로운 참가자를 받고 있지 않습니다."
            );
        }

        Integer maxParticipants = meeting.getMaxParticipants();

        if(maxParticipants != null)
        {
            int currentParticipants =
                    participantMapper.countParticipant(
                            meeting.getMeetingId()
                    );

            if(currentParticipants >= maxParticipants)
            {
                throw new IllegalStateException(
                        "모임의 참가 인원이 모두 찼습니다."
                );
            }
        }




        // QUICK_VOTE에서 정상적으로 나갔던 사용자 재입장
        if (
                existingParticipant != null
                &&
                existingParticipant.getParticipantStatus()
                == ParticipantStatus.LEFT
        )
        {
            if(
                    meeting.getMeetingType() != MeetingType.QUICK_VOTE
                            &&
                    meeting.getMeetingType() != MeetingType.QUICK_FIXED
            )
            {
                throw new IllegalStateException(
                        "친구방의 참가 상태를 확인해주세요"
                );
            }


            int updatedRows =
                    participantMapper
                            .reactivateLeftParticipant(
                                    existingParticipant
                                            .getParticipantId()
                            );


            if (updatedRows != 1) {

                throw new IllegalStateException(
                        "모임 재입장 처리에 실패했습니다."
                );
            }


            return new MeetingJoinResponse(
                    meeting.getMeetingId(),
                    true,
                    meeting.getMeetingType()
            );
        }


        // 6. 참가 이력이 없는 사용자 등록
        MeetingParticipant participant =
                new MeetingParticipant();


        participant.setMeetingId(
                meeting.getMeetingId()
        );


        participant.setUserId(
                userId
        );


        participant.setParticipantStatus(
                ParticipantStatus.ACTIVE
        );


        participant.setInputStatus(
                InputStatus.DRAFT
        );


        participantMapper.insertParticipant(
                participant
        );


        // 7. 신규 참가 결과 반환
        return new MeetingJoinResponse(
                meeting.getMeetingId(),
                true,
                meeting.getMeetingType()
        );
    }


    // ============================================================
    // 최종 후보 확정
    // ============================================================

    @Transactional
    public void confirmFinalCandidate(
            Long meetingId,
            Long hostUserId,
            FinalCandidateRequest request
    ) {

        if (request.getCandidateId() == null) {

            throw new IllegalArgumentException(
                    "candidateId는 필수입니다."
            );
        }


        // 모임 조회
        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        // 방장 확인
        if (
                !meeting.getHostUserId()
                        .equals(hostUserId)
        ) {

            throw new IllegalStateException(
                    "방장만 최종 장소를 확정할 수 있습니다."
            );
        }


        // VOTING 상태인지 확인
        if (
                meeting.getStatus()
                        != MeetingStatus.VOTING
        ) {

            throw new IllegalStateException(
                    "현재 최종 후보를 확정할 수 없는 상태입니다."
            );
        }


        // QUICK_VOTE는 전체 참가자의 과반수가 투표해야 확정 가능
        if (
                meeting.getMeetingType()
                        == MeetingType.QUICK_VOTE
        ) {

            int totalParticipants =
                    participantMapper.countParticipant(
                            meetingId
                    );


            int totalVotes =
                    voteMapper.countVotesByMeetingId(
                            meetingId
                    );


            int requiredVotes =
                    (totalParticipants / 2) + 1;


            if (
                    totalParticipants == 0
                            ||
                            totalVotes < requiredVotes
            ) {

                throw new IllegalStateException(
                        "과반수의 투표가 완료되어야 장소를 확정할 수 있습니다."
                );
            }
        }


        // 후보 확인
        MeetingCandidate candidate =
                candidateMapper.selectActiveCandidate(
                        meetingId,
                        request.getCandidateId()
                );


        if (candidate == null) {

            throw new IllegalArgumentException(
                    "유효하지 않은 후보지입니다."
            );
        }

        // QUICK_VOTE는 단독 최다 득표 후보만 확정 가능
        if (meeting.getMeetingType() == MeetingType.QUICK_VOTE) {

            List<CandidateVoteResult> voteResults =
                    voteMapper.selectVoteResults(
                            meetingId
                    );

            if (
                    voteResults == null
                            ||
                            voteResults.isEmpty()
            ) {

                throw new IllegalStateException(
                        "투표 결과를 확인할 수 없습니다."
                );
            }


            int maxVoteCount =
                    voteResults.stream()
                            .mapToInt(
                                    CandidateVoteResult::getVoteCount
                            )
                            .max()
                            .orElse(0);


            // 후보에게 들어간 표가 하나도 없는 경우
            if (maxVoteCount == 0) {

                throw new IllegalStateException(
                        "득표한 후보가 없어 장소를 확정할 수 없습니다."
                );
            }


            List<CandidateVoteResult> topCandidates =
                    voteResults.stream()
                            .filter(
                                    result ->
                                            result.getVoteCount()
                                                    == maxVoteCount
                            )
                            .toList();


            // 최다 득표 후보가 여러 명이면 동점
            if (topCandidates.size() != 1) {

                throw new IllegalStateException(
                        "최다 득표 후보가 동점이라 장소를 확정할 수 없습니다."
                );
            }


            Long topCandidateId =
                    topCandidates
                            .get(0)
                            .getCandidateId();


            // 요청으로 들어온 후보가 실제 1등인지 확인
            if (
                    !topCandidateId.equals(
                            request.getCandidateId()
                    )
            ) {

                throw new IllegalStateException(
                        "최다 득표한 장소만 확정할 수 있습니다."
                );
            }
        }


        // 최종 장소 확정
        int result =
                meetingMapper.updateFinalCandidate(
                        meetingId,
                        request.getCandidateId(),
                        MeetingStatus.CONFIRMED
                );


        if (result == 0) {

            throw new IllegalStateException(
                    "최종 장소 확정에 실패했습니다."
            );
        }
    }


    // ============================================================
    // 투표 시작
    // ============================================================

    @Transactional
    public void startVoting(
            Long meetingId,
            Long hostUserId
    ) {

        // 1. 모임 존재 확인
        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        // 2. 방장 확인
        if (
                !meeting.getHostUserId()
                        .equals(hostUserId)
        ) {

            throw new IllegalStateException(
                    "방장만 투표를 시작할 수 있습니다."
            );
        }


        // 3. 현재 상태 확인
        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {

            throw new IllegalStateException(
                    "투표를 시작할 수 없는 모임 상태입니다."
            );
        }


        // 4. 전원 위치 제출 확인
        boolean allSubmitted =
                participantService.isAllSubmitted(
                        meetingId
                );


        if (!allSubmitted) {

            throw new IllegalStateException(
                    "모든 참가자의 위치 입력이 완료되지 않았습니다."
            );
        }


        // 5. 후보 존재 확인
        boolean candidateExists =
                candidateService.existsCandidate(
                        meetingId
                );


        if (!candidateExists) {

            throw new IllegalStateException(
                    "등록된 후보지가 없습니다."
            );
        }


        // 6. VOTING으로 변경
        meetingMapper.updateMeetingStatus(
                meetingId,
                MeetingStatus.VOTING
        );
    }

    // ============================================================
// 고정 번개방 참가 모집 마감
// ============================================================

    @Transactional
    public void closeFixedRecruitment(
            Long meetingId,
            Long hostUserId
    ) {
        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );

        if (meeting == null) {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }

        if (
                meeting.getMeetingType()
                        != MeetingType.QUICK_FIXED
        ) {
            throw new IllegalStateException(
                    "고정 번개방만 참가 모집을 마감할 수 있습니다."
            );
        }

        if (
                !Objects.equals(
                        meeting.getHostUserId(),
                        hostUserId
                )
        ) {
            throw new AccessDeniedException(
                    "방장만 참가 모집을 마감할 수 있습니다."
            );
        }

        if (
                meeting.getStatus()
                        == MeetingStatus.RECRUITMENT_CLOSED
        ) {
            return;
        }

        if (
                meeting.getStatus()
                        != MeetingStatus.INPUT_OPEN
        ) {
            throw new IllegalStateException(
                    "현재 상태에서는 참가 모집을 마감할 수 없습니다."
            );
        }

        int updatedRows =
                meetingMapper.updateMeetingStatus(
                        meetingId,
                        MeetingStatus.RECRUITMENT_CLOSED
                );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "참가 모집 마감에 실패했습니다."
            );
        }
    }


    // ============================================================
    // 모임방 조회
    // ============================================================

    public MeetingRoomResponse getMeetingRoom(
            Long meetingId,
            Long userId
    ) {

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        int participantCount =
                participantMapper
                        .countParticipantByMeetingAndUser(
                                meetingId,
                                userId
                        );


        if (participantCount == 0) {

            throw new IllegalArgumentException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        return new MeetingRoomResponse(
                meeting.getMeetingId(),
                meeting.getHostUserId(),
                meeting.getTitle(),
                meeting.getInviteCode(),
                meeting.getDesiredEndAt(),
                meeting.getMeetingStartAt(),
                meeting.getMeetingType(),
                meeting.getMaxParticipants(),
                meeting.getStatus(),
                meeting.getFinalCandidateId()
        );
    }


    // ============================================================
    // 모임 조회
    // ============================================================

    public Meeting getMeeting(
            Long meetingId,
            Long userId
    ) {

        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null) {

            throw new IllegalArgumentException(
                    "존재하지 않는 모임입니다."
            );
        }


        int participantCount =
                participantMapper
                        .countParticipantByMeetingAndUser(
                                meetingId,
                                userId
                        );


        if (participantCount == 0) {

            throw new IllegalStateException(
                    "해당 모임의 참가자가 아닙니다."
            );
        }


        return meeting;
    }


    // ============================================================
    // 내 모임 조회
    // ============================================================

    public List<MyMeetingResponse> getMyMeetings(Long userId) {
        return meetingMapper.selectMyMeetings(
                userId
        );
    }

    // ============================================================
    // 내 번개 모임 조회
    // ============================================================

    public List<MyMeetingResponse> getMyQuickMeetings(Long userId) {
        return meetingMapper.selectMyQuickMeetings(userId);
    }


    // ============================================================
    // 종료된 모임 삭제
    // ============================================================

    @Transactional
    public int deleteExpiredMeetings() {

        return meetingMapper.deleteExpiredMeetings();
    }

    // ============================================================
    // 번개 모임 목록 조회
    // ============================================================

    public List<QuickMeetingResponse> getQuickVoteMeetings(
            String keyword
    ) {

        String searchKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();


        return meetingMapper.selectQuickVoteMeetings(
                searchKeyword
        );
    }

    @Transactional
    public void cancelQuickFixedSetup(
            Long meetingId,
            Long userId
    ) {
        Meeting meeting =
                meetingMapper.findById(
                        meetingId
                );


        if (meeting == null)
        {
            throw new IllegalArgumentException(
                    "모임을 찾을 수 없습니다."
            );
        }


        // 고정 번개만 가능
        if (
                meeting.getMeetingType()
                        !=
                        MeetingType.QUICK_FIXED
        )
        {
            throw new IllegalStateException(
                    "고정 번개방만 생성 취소할 수 있습니다."
            );
        }


        // 방장만 가능
        if (
                !meeting.getHostUserId()
                        .equals(userId)
        )
        {
            throw new IllegalStateException(
                    "방장만 모임 생성을 취소할 수 있습니다."
            );
        }


        MeetingParticipant hostParticipant =
                meetingParticipantMapper
                        .findByMeetingIdAndUserId(
                                meetingId,
                                userId
                        );


        if (hostParticipant == null)
        {
            throw new IllegalStateException(
                    "방장 참가자 정보를 찾을 수 없습니다."
            );
        }


        /*
         * 이미 귀가지를 등록했다는 것은
         * 고정 번개 생성 절차를 완료했다는 뜻.
         */
        if (
                hostParticipant.getReturnLatitude()
                        != null
                        ||
                        hostParticipant.getReturnLongitude()
                                != null
        )
        {
            throw new IllegalStateException(
                    "이미 생성이 완료된 모임입니다."
            );
        }


        /*
         * 혹시 다른 사람이 이미 참가했다면
         * 방장이 임의로 통째로 삭제하지 못하게 막음.
         */
        int activeParticipantCount =
                meetingParticipantMapper
                        .countActiveByMeetingId(
                                meetingId
                        );
        if (activeParticipantCount > 1)
        {
            throw new IllegalStateException(
                    "이미 다른 참가자가 있어 모임 생성을 취소할 수 없습니다."
            );
        }
        /*
         * meetings.final_candidate_id가
         * meeting_candidates를 참조하고 있을 수 있으므로
         * 먼저 연결 해제
         */
        meetingMapper.clearFinalCandidate(
                meetingId
        );
        /*
         * 후보가 participant를 참조할 수 있기 때문에
         * 후보 → 참가자 → 모임 순서
         */
        meetingCandidateMapper.deleteByMeetingId(
                meetingId
        );
        meetingParticipantMapper.deleteByMeetingId(
                meetingId
        );


        meetingMapper.deleteById(
                meetingId
        );
    }
}
