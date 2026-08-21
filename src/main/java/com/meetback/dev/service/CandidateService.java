package com.meetback.dev.service;

import com.meetback.dev.domain.MeetingCandidate;
import com.meetback.dev.repository.CandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateMapper candidateMapper;

    public boolean existsCandidate(Long meetingId)
    {
        return candidateMapper.countActiveCandidate(meetingId) > 0;
    }

}
