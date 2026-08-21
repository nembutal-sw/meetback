package com.meetback.dev.repository;

import com.meetback.dev.domain.Social;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SocialMapper {


    // 동일 카카오계정 중복가입 확인
    Social selectByProviderAndProviderId(

            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

     Social selectByUserIdAndProvider(
            //한 사용자가 동일 provider를 중복 연결하는 것 방지
            @Param("userId") Long userId,
            @Param("provider") String provider
    );

    // 신규 소셜 로그인 정보 저장
   int insertSocial(Social social);
}
