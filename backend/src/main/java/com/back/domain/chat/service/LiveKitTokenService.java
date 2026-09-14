package com.back.domain.chat.service;

import com.back.domain.chat.dto.LiveKitTokenResponse;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * LiveKit 입장 토큰(JWT) 발급
 * - 실제 음성/텍스트/이동 데이터는 이 토큰을 들고 브라우저가 LiveKit에 직접 접속해서 처리
 */
@Service
public class LiveKitTokenService {

    @Value("${livekit.url}")
    private String url;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    public LiveKitTokenResponse createToken(String roomName, String identity, String displayName) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        token.setName(displayName);
        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        return LiveKitTokenResponse.builder()
                .token(token.toJwt())
                .url(url)
                .build();
    }
}
