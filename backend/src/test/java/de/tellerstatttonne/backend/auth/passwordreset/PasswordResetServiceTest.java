package de.tellerstatttonne.backend.auth.passwordreset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.tellerstatttonne.backend.auth.RefreshTokenRepository;
import de.tellerstatttonne.backend.config.AppProperties;
import de.tellerstatttonne.backend.mail.MailService;
import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private MailService mailService;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        tokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
        refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        mailService = Mockito.mock(MailService.class);
        AppProperties props = new AppProperties(new AppProperties.Frontend("http://localhost:4200"));
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        service = new PasswordResetService(
            userRepository, tokenRepository, refreshTokenRepository,
            passwordEncoder, mailService, props, eventPublisher
        );
    }

    @Test
    void initiate_unbekannteMail_kontaktiertNiemanden() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.initiate("ghost@example.com");

        verifyNoInteractions(mailService);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void initiate_bekannteMail_loeschtAlteTokensUndVersendetMail() {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setFirstName("Anna");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        service.initiate("User@Example.com");

        verify(tokenRepository).deleteByUserId(42L);
        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetTokenEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(25, ChronoUnit.MINUTES));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(35, ChronoUnit.MINUTES));

        verify(mailService).sendHtml(eq("user@example.com"), anyString(), anyString(), anyString());
    }

    @Test
    void reset_ungueltigerToken_wirft() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("does-not-exist", "neuesPasswort1"))
            .isInstanceOf(PasswordResetService.InvalidTokenException.class);
    }

    @Test
    void reset_abgelaufen_wirft() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUserId(1L);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reset("raw", "neuesPasswort1"))
            .isInstanceOf(PasswordResetService.InvalidTokenException.class);
    }

    @Test
    void reset_bereitsBenutzt_wirft() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUserId(1L);
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        token.setUsedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reset("raw", "neuesPasswort1"))
            .isInstanceOf(PasswordResetService.InvalidTokenException.class);
    }

    @Test
    void reset_happyPath_aendertPasswortUndWiderruftRefreshTokens() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUserId(7L);
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setPasswordHash("alt");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NeuesPasswort1!")).thenReturn("hash-neu");

        service.reset("raw", "NeuesPasswort1!");

        assertThat(user.getPasswordHash()).isEqualTo("hash-neu");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository, times(1)).save(token);
        verify(refreshTokenRepository).revokeAllForUser(7L);
    }
}
