package com.meetback.dev.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthCheckResponse
{

   private Long userId;
   private String email;
   private String nickname;
   private String role;
}
