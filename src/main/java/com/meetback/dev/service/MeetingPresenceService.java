package com.meetback.dev.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MeetingPresenceService {

    /*
     * sessionId
     *      ↓
     * 어느 모임의 어느 사용자인지
     */
    private final Map<String, SessionPresence>
            sessions = new HashMap<>();

    /*
     * meetingId + userId
     *      ↓
     * 현재 WebSocket 세션 개수
     *
     * Chrome + Edge
     * 또는
     * 같은 브라우저 탭 2개
     *
     * 같은 경우도 처리하기 위함.
     */
    private final Map<PresenceKey, Integer>
            connectionCounts = new HashMap<>();

    /*
     * WebSocket 구독 시작
     *
     * 반환값:
     * null -> 이미 ONLINE이었음
     * 객체 -> OFF -> ON으로 바뀜
     */
    public synchronized PresenceChange connect(
            String sessionId,
            Long meetingId,
            Long userId
    ) {

        if (sessions.containsKey(sessionId)) {
            return null;
        }


        SessionPresence session =
                new SessionPresence(
                        meetingId,
                        userId
                );


        sessions.put(
                sessionId,
                session
        );


        PresenceKey key =
                new PresenceKey(
                        meetingId,
                        userId
                );


        int nextCount =
                connectionCounts
                        .getOrDefault(
                                key,
                                0
                        )
                        + 1;


        connectionCounts.put(
                key,
                nextCount
        );


        /*
         * 첫 번째 세션일 때만
         *
         * OFF -> ON
         */
        if (nextCount == 1) {

            return new PresenceChange(
                    meetingId,
                    userId,
                    true
            );
        }


        return null;
    }


    /*
     * WebSocket 연결 종료
     */
    public synchronized PresenceChange disconnect(
            String sessionId
    ) {

        SessionPresence session =
                sessions.remove(
                        sessionId
                );


        if (session == null) {
            return null;
        }


        PresenceKey key =
                new PresenceKey(
                        session.meetingId(),
                        session.userId()
                );


        int currentCount =
                connectionCounts
                        .getOrDefault(
                                key,
                                0
                        );


        int nextCount =
                currentCount - 1;


        /*
         * 모든 탭 / WebSocket이 끊어졌음
         *
         * ON -> OFF
         */
        if (nextCount <= 0) {

            connectionCounts.remove(
                    key
            );


            return new PresenceChange(
                    session.meetingId(),
                    session.userId(),
                    false
            );
        }


        /*
         * 다른 탭이 아직 연결되어 있음
         */
        connectionCounts.put(
                key,
                nextCount
        );


        return null;
    }


    public synchronized boolean isOnline(
            Long meetingId,
            Long userId
    ) {

        PresenceKey key =
                new PresenceKey(
                        meetingId,
                        userId
                );


        return connectionCounts
                .getOrDefault(
                        key,
                        0
                )
                > 0;
    }


    private record PresenceKey(
            Long meetingId,
            Long userId
    ) {
    }


    private record SessionPresence(
            Long meetingId,
            Long userId
    ) {
    }


    public record PresenceChange(
            Long meetingId,
            Long userId,
            boolean online
    ) {
    }



}
