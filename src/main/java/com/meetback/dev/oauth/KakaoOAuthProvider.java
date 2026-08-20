package com.meetback.dev.oauth;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

    @Component
    public class KakaoOAuthProvider {

        private final RestClient restClient;

        private final String clientId;
        private final String redirectUri;

        public KakaoOAuthProvider(
                @Value ("${kakao.client-id}") String clientId,
                @Value("${kakao.redirect-uri}") String redirectUri
        ) {

            this.restClient = RestClient.create();
            this.clientId = clientId;
            this.redirectUri = redirectUri;
        }


        // 카카오 인가 코드 -> 카카오 Access Token
        public String requestAccessToken(String code) {

            MultiValueMap<String, String> form =
                    new LinkedMultiValueMap<>();

            form.add(
                    "grant_type",
                    "authorization_code"
            );

            form.add(
                    "client_id",
                    clientId
            );

            form.add(
                    "redirect_uri",
                    redirectUri
            );

            form.add(
                    "code",
                    code
            );


            KakaoTokenResponse response =
                    restClient.post()
                            .uri("https://kauth.kakao.com/oauth/token")
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .body(form)
                            .retrieve()
                            .body(KakaoTokenResponse.class);


            if (response == null
                    || response.getAccessToken() == null) {

                throw new IllegalArgumentException(
                        "카카오 Access Token 발급에 실패했습니다."
                );
            }


            return response.getAccessToken();
        }


        // 카카오 Access Token -> 사용자 정보
        public KakaoUserInfo getUserInfo(
                String kakaoAccessToken
        ) {

            JsonNode response =
                    restClient.get()
                            .uri("https://kapi.kakao.com/v2/user/me")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + kakaoAccessToken
                            )
                            .retrieve()
                            .body(JsonNode.class);


            if (response == null
                    || response.get("id") == null) {

                throw new IllegalArgumentException(
                        "카카오 사용자 정보 조회에 실패했습니다."
                );
            }


            KakaoUserInfo userInfo =
                    new KakaoUserInfo();


            userInfo.setProviderId(
                    response.get("id").asString()
            );


            JsonNode kakaoAccount =
                    response.path("kakao_account");


            if (kakaoAccount.has("email")) {

                userInfo.setEmail(
                        kakaoAccount
                                .get("email").asString()
                );
            }


            if (kakaoAccount.has(
                    "is_email_verified"
            )) {

                userInfo.setEmailVerified(
                        kakaoAccount
                                .get("is_email_verified")
                                .asBoolean()
                );
            }


            JsonNode profile =
                    kakaoAccount.path("profile");


            if (profile.has("nickname")) {

                userInfo.setName(
                        profile
                                .get("nickname").asString()
                );
            }


            return userInfo;
        }
    }

