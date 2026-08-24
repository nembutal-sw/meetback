package com.meetback.dev.security;

import com.meetback.dev.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    )
    {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if(accessor == null)
        {
            return message;
        }

        if(StompCommand.CONNECT.equals(
                accessor.getCommand()
        ))
        {
            String authorization = accessor.getFirstNativeHeader(
                    "Authorization"
            );

            if(authorization == null
                || !authorization.startsWith("Bearer"))
            {
                throw new BadCredentialsException(
                        "Access Token이 필요합니다."
                );
            }

            String token = authorization.substring(7);

            if(!jwtProvider.validateToken(token))
            {
                throw new BadCredentialsException(
                        "유효하지 않은 토큰입니다."
                );
            }

            if(!"ACCESS".equals(
                    jwtProvider.getTokenType(token)
            ))
            {
                throw new BadCredentialsException(
                        "Access Token만 사용할 수 있습니다."
                );
            }

            Long userId = jwtProvider.getUserId(token);

            String role = jwtProvider.getRole(token);

            AuthenticatedUser user = new AuthenticatedUser(
                    userId,
                    role
            );

            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(
                            new SimpleGrantedAuthority(
                                    authority
                            )
                    )
            );

            accessor.setUser(authentication);
        }
        return message;
    }

}
