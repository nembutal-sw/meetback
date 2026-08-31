package com.meetback.dev.WebSocket;

import com.meetback.dev.domain.MeetingEventType;
import com.meetback.dev.realtime.event.RealtimeEvent;
import com.meetback.dev.realtime.publisher.RealtimeEventPublisher;
import com.meetback.dev.security.AuthenticatedUser;
import com.meetback.dev.service.MeetingPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MeetingPresenceEventListener {

    private final MeetingPresenceService presenceService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /*
     * ============================================================
     * 모임 Topic 구독
     * ============================================================
     */
    @EventListener
    public void handleSubscribe(
            SessionSubscribeEvent event
    )
    {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(
                        event.getMessage()
                );

        String destination = accessor.getDestination();

        Long meetingId = extractMeetingId(
                destination
        );

        /*
         * MeetBack 모임 chat Topic이 아니면 무시
         */
        if(meetingId == null)
        {
            return;
        }

        Principal principal = event.getUser();

        if(!(principal instanceof Authentication authentication))
        {
            return;
        }

        Object principalObject = authentication.getPrincipal();

        if(!(principalObject instanceof AuthenticatedUser user))
        {
            return;
        }

        String sessionId = accessor.getSessionId();

        if(sessionId == null)
        {
            return;
        }

        MeetingPresenceService.PresenceChange change =
                presenceService.connect(
                        sessionId,
                        meetingId,
                        user.userId()
                );

        /*
         * 가장 최근에 구독한 탭을 활성 세션으로 지정합니다.
         */
        MeetingPresenceService.ActiveRoomSession previousSession =
                presenceService.claimActiveSession(
                        sessionId,
                        meetingId,
                        user.userId()
        );

        String activeTabId =
                accessor.getFirstNativeHeader(
                        "x-room-tab-id"
                );

        if (
                previousSession != null
                &&
                activeTabId != null
                &&
                !activeTabId.isBlank()
        ) {
            broadcastRoomSessionReplaced(
                    previousSession.meetingId(),
                    user.userId(),
                    activeTabId
            );
        }

        /*
         * 이미 ONLINE이었다면 방송할 필요 없음
         */
        if(change == null)
        {
            return;
        }

        broadcastPresence(
                change
        );
    }

    /*
     * ============================================================
     * WebSocket 종료
     * ============================================================
     */
    @EventListener
    public void handleDisconnect(
            SessionDisconnectEvent event
    )
    {
        MeetingPresenceService.PresenceChange change =
                presenceService.disconnect(
                        event.getSessionId()
                );

        /*
         * 같은 사용자의 다른 탭이 살아있다면
         * OFF가 아니므로 방송하지 않음
         */
        if(change == null)
        {
            return;
        }

        broadcastPresence(
                change
        );
    }

    private void broadcastPresence(
            MeetingPresenceService.PresenceChange change
    )
    {
        String eventType = MeetingEventType.PRESENCE_UPDATED.name();

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        change.meetingId(),
                        change.userId(),
                        Map.of(
                                "messageType", "PRESENCE",
                                "eventType", eventType,
                                "userId", change.userId(),
                                "online", change.online()
                        )
                )
        );

    }

    private void broadcastRoomSessionReplaced(
            Long meetingId,
            Long userId,
            String activeTabId
    )
    {
        String eventType = MeetingEventType.ROOM_SESSION_REPLACED.name();

        realtimeEventPublisher.publish(
                RealtimeEvent.meetingBroadcast(
                        eventType,
                        meetingId,
                        userId,
                        Map.of(
                                "messageType", "EVENT",
                                "eventType", eventType,
                                "meetingId", meetingId,
                                "userId", userId,
                                "activeTabId", activeTabId
                        )
                )
        );

    }

    /*
     * /topic/meetings/33/chat
     *
     *          ↓
     *
     * 33
     */
    private Long extractMeetingId(
            String destination
    )
    {
        if(destination == null)
        {
            return null;
        }

        String prefix = "/topic/meetings/";

        String suffix = "/chat";

        if(
                !destination.startsWith(
                        prefix
                ) || !destination.endsWith(
                        suffix
                )
        )
        {
            return null;
        }

        String text =
                destination.substring(
                        prefix.length(),
                        destination.length()
                            - suffix.length()
                );

        try{
            return Long.valueOf(
                    text
            );
        }catch(NumberFormatException e)
        {
            return null;
        }
    }
}
