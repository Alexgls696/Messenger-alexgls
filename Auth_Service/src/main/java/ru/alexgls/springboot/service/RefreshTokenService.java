package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {


    @Value("${jwt.refresh.expirationMs}")
    private Long refreshExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteTokenByUserId(int id) {
        refreshTokenRepository.deleteByUserId(id);
    }

    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    public RefreshToken createRefreshToken(Integer userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setExpiresDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiresDate()
                .compareTo(Instant.now()) < 0) {
            refreshTokenRepository
                    .delete(refreshToken);
            throw new RuntimeException("\"Refresh token was expired." +
                    "Please make a new signin request.\"");
        }
        return refreshToken;
    }
}
