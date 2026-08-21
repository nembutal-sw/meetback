package com.meetback.dev.place.Controller;

import com.meetback.dev.place.client.KakaoLocalClient;
import com.meetback.dev.place.dto.PlaceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlaceController {

    private final KakaoLocalClient kakaoLocalClient;

    @GetMapping("/places")
    public List<PlaceDTO> search(
            @RequestParam String query) {

        return kakaoLocalClient.search(query);
    }
}