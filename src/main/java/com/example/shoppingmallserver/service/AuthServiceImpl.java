package com.example.shoppingmallserver.service;

import com.example.shoppingmallserver.base.Role;
import com.example.shoppingmallserver.dto.EmailNotificationDto;
import com.example.shoppingmallserver.entity.auth.Auth;
import com.example.shoppingmallserver.exception.*;
import com.example.shoppingmallserver.feign.NotificationFeignClient;
import com.example.shoppingmallserver.redis.entity.VerificationCode;
import com.example.shoppingmallserver.redis.repository.VerificationCodeRepository;
import com.example.shoppingmallserver.repository.AuthRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Random;

/**
 * 사용자 인증 관련 서비스를 제공하는 클래스입니다.
 * 이 클래스에서는 사용자 인증과 관련된 주요 기능들을 구현합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * 로깅을 위한 Logger 객체입니다. AuthServiceImpl 클래스의 정보를 이용해 LoggerFactory에서 Logger 인스턴스를 가져옵니다.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * 알림과 관련된 기능을 제공하는 Feign 클라이언트입니다.
     * 이 클라이언트를 사용해 알림 서비스와 통신할 수 있습니다.
     */
    private final NotificationFeignClient notificationFeignClient;

    /**
     * 사용자 인증 정보를 관리하는 레포지토리입니다.
     * 이 레포지토리를 통해 사용자 인증 정보의 CRUD 작업을 수행할 수 있습니다.
     */
    private final AuthRepository authRepository;

    /**
     * 사용자 인증 코드를 관리하는 레포지토리입니다.
     * 이 레포지토리를 통해 인증 코드의 생성 및 검증 등의 작업을 수행할 수 있습니다.
     */
    private final VerificationCodeRepository verificationCodeRepository;

    /**
     * 비밀번호를 암호화하고 검증하는 역할을 하는 인코더입니다.
     */
    private final PasswordEncoder passwordEncoder;

    @Override
    public Auth register(String email, String password, Role role) {

        // 이메일로 사용자 찾기
        Optional<Auth> auth = authRepository.findByEmail(email);

        // 사용자가 이미 존재하는 경우 예외 발생
        if (auth.isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 사용자 정보 생성
        Auth newUser = Auth.builder()
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();

        // 사용자 저장
        return authRepository.save(newUser);
    }

    /**
     * 사용자의 이메일과 비밀번호를 검증하고, 검증이 성공하면 사용자의 고유 식별자를 반환합니다.
     *
     * @param email 검증할 사용자의 이메일
     * @param password 검증할 사용자의 비밀번호
     * @return 사용자의 고유 식별자
     * @throws AuthNotFoundException 이메일에 해당하는 사용자를 찾을 수 없을 때 발생
     * @throws InvalidPasswordException 입력받은 비밀번호와 저장된 비밀번호가 일치하지 않을 때 발생
     */
    @Override
    public Long verifyUser(String email, String password) {
        // 이메일로 사용자 조회
        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new AuthNotFoundException(email));

        // 입력받은 비밀번호와 저장된 비밀번호 비교
        if (!passwordEncoder.matches(password, auth.getPassword())) {
            throw new InvalidPasswordException(email);
        }

        // 비밀번호가 일치하면 사용자의 고유 식별자 반환
        return auth.getUserIdx();
    }

    /**
     * 비밀번호 재설정을 위한 인증번호를 요청하는 메소드
     *
     * @param email 사용자의 이메일 주소
     */
    @Override
    public void requestPasswordReset(String email) {
        try {
            // 자연수 4자리 인증번호 생성 (0부터 9999까지)
            String verificationCode = String.format("%04d", new Random().nextInt(10000));

            // 이메일 전송을 위한 DTO 객체 생성
            EmailNotificationDto dto = new EmailNotificationDto(
                    email,
                    "비밀번호 재설정 인증번호 안내",
                    "비밀번호 재설정을 위한 인증번호는 " + verificationCode + " 입니다."
            );

            // 이메일 전송
            ResponseEntity<String> response = notificationFeignClient.sendEmailVerificationCode(dto);

            // 이메일 전송이 성공하면 인증번호 저장
            if (response.getStatusCode().is2xxSuccessful()) {

                // 인증번호 저장을 위한 VerificationCode 객체 생성
                VerificationCode verificationCodeEntity = new VerificationCode(
                        email,
                        verificationCode
                );

                // 인증번호 저장
                verificationCodeRepository.save(verificationCodeEntity);
            }
        }
        catch (Exception e) {
            logger.error("비밀번호 재설정 인증번호 전송 중 알 수 없는 예외 발생", e);
            throw e;
        }
    }

    /**
     * 사용자가 제공한 인증 코드를 검증하고, 검증이 성공하면 사용자의 비밀번호를 새로운 비밀번호로 재설정합니다.
     * 인증 코드가 일치하지 않거나, 인증 코드가 존재하지 않는 경우 예외가 발생합니다.
     *
     * @param email 사용자의 이메일
     * @param code 사용자가 제공한 인증 코드
     * @param newPassword 사용자의 새로운 비밀번호
     * @throws AuthNotFoundException 이메일에 해당하는 사용자가 존재하지 않는 경우
     * @throws VerificationCodeNotFoundException 제공된 이메일에 해당하는 인증 코드가 존재하지 않는 경우
     * @throws InvalidVerificationCodeException 제공된 인증 코드가 저장된 인증 코드와 일치하지 않는 경우
     */
    @Override
    public void verifyCodeAndResetPassword(String email, String code, String newPassword) {

        // 이메일로 전송된 유저 확인
        Auth auth = authRepository.findByEmail(email)
                .orElseThrow(() -> new AuthNotFoundException(email));

        // 이메일로 전송된 인증 코드 쿼리 확인
        VerificationCode storedCode = verificationCodeRepository.findById(email)
                .orElseThrow(() -> new VerificationCodeNotFoundException(email));

        // 입력받은 인증 코드와 저장된 인증 코드 비교
        if (!storedCode.getCode().equals(code)) {
            throw new InvalidVerificationCodeException(email);
        }

        // 인증 코드가 일치하면 비밀번호 변경
        auth.updatePassword(passwordEncoder.encode(newPassword));

        // Delete the verification code
        verificationCodeRepository.delete(storedCode);
    }


    /**
     * 사용자의 비밀번호를 변경하는 메소드
     *
     * @param email 사용자의 이메일 주소
     * @param oldPassword 사용자의 현재 비밀번호
     * @param newPassword 사용자가 설정하려는 새 비밀번호
     * @throws AuthNotFoundException 해당 이메일을 가진 사용자를 찾을 수 없을 경우 발생
     * @throws InvalidPasswordException 기존 비밀번호가 일치하지 않을 경우 발생
     */
    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        logger.info("비밀번호 변경 요청. 이메일: " + email);

        Auth auth = authRepository.findByEmail(email).orElseThrow(() -> {
            logger.error("해당 이메일을 가진 사용자를 찾을 수 없음: " + email);
            return new AuthNotFoundException(email);
        });

        // 기존 비밀번호 확인
        if (!passwordEncoder.matches(oldPassword, auth.getPassword())) {
            logger.error("비밀번호 불일치: " + email);
            throw new InvalidPasswordException(email);
        }

        // 새 비밀번호 암호화 후 저장
        auth.updatePassword(passwordEncoder.encode(newPassword));

        logger.info("비밀번호 변경 완료: " + email);
    }

    @Override
    public void deleteAccount(int identifier) {
        // TODO: Not yet implemented
    }
}