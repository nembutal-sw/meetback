package com.meetback.dev.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleIdentityProvider {

    private final boolean configured;
    private final GoogleIdTokenVerifier tokenVerifier;

    public GoogleIdentityProvider(
            @Value("${google.client-id:}") String clientId
    ) {

        configured =
                clientId != null
                        && !clientId.isBlank();

        // Google 공식 검증기는 공개키 서명, 발급자(iss), 만료시간(exp)를 확인한다.
        // audience를 MeetBack Client ID로 고정해 다른 앱용 토큰의 재사용도 차단한다.
        tokenVerifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(
                                Collections.singletonList(
                                        clientId
                                )
                        )
                        .build();
    }

    public GoogleUserInfo verifyIdToken(
            String credential
    ) {

        if (!configured) {

            throw new IllegalStateException(
                    "GOOGLE_CLIENT_ID가 설정되지 않았습니다."
            );
        }

        if (credential == null
                || credential.isBlank()) {

            throw new IllegalArgumentException(
                    "Google 인증 정보가 없습니다."
            );
        }

        try {

            GoogleIdToken idToken =
                    tokenVerifier.verify(
                            credential
                    );

            if (idToken == null) {

                throw new IllegalArgumentException(
                        "유효하지 않은 Google 인증 정보입니다."
                );
            }

            GoogleIdToken.Payload payload =
                    idToken.getPayload();

            // 이메일은 변경될 수 있으므로 사용자 조회 키로 사용하지 않는다.
            // Google이 계정마다 발급하는 불변 식별자인 sub를 providerId로 저장한다.
            if (payload.getSubject() == null
                    || payload.getSubject().isBlank()) {

                throw new IllegalArgumentException(
                        "Google 사용자 식별자를 확인할 수 없습니다."
                );
            }

            if (payload.getEmail() == null
                    || payload.getEmail().isBlank()) {

                throw new IllegalArgumentException(
                        "Google 계정 이메일을 확인할 수 없습니다."
                );
            }

            if (!Boolean.TRUE.equals(
                    payload.getEmailVerified()
            )) {

                throw new IllegalArgumentException(
                        "인증되지 않은 Google 이메일입니다."
                );
            }

            GoogleUserInfo userInfo =
                    new GoogleUserInfo();

            userInfo.setProviderId(
                    payload.getSubject()
            );

            userInfo.setEmail(
                    payload.getEmail()
            );

            userInfo.setEmailVerified(
                    payload.getEmailVerified()
            );

            Object name =
                    payload.get("name");

            if (name instanceof String nickname
                    && !nickname.isBlank()) {

                userInfo.setNickname(
                        nickname.trim()
                );
            }

            return userInfo;

        } catch (GeneralSecurityException
                 | IOException e) {

            throw new IllegalArgumentException(
                    "Google 인증 정보 검증에 실패했습니다.",
                    e
            );
        }
    }
}
