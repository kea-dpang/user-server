package com.example.shoppingmallserver.service;

import com.example.shoppingmallserver.dto.response.user.AdminReadUserListResponseDto;
import com.example.shoppingmallserver.dto.response.feign.QnaAuthorDto;
import com.example.shoppingmallserver.entity.user.*;
import com.example.shoppingmallserver.exception.*;
import com.example.shoppingmallserver.feign.mileage.MileageFeignClient;
import com.example.shoppingmallserver.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;

    /**
     * 마일리지와 관련된 기능을 제공하는 Feign 클라이언트입니다.
     * 이 클라이언트를 사용해 마일리지 서비스와 통신할 수 있습니다.
     */
    private final MileageFeignClient mileageFeignClient;

    @Override
    public void register(String email, Long employeeNumber, String name, LocalDate joinDate) {

        // 이메일로 사용자를 찾고 사용자가 이미 존재하는 경우 예외 발생
        // 변수 인라인화
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException(email);
                });

        // 사용자 정보 생성
        User newUser = User.builder()
                .email(email)
                .status(UserStatus.USER)
                .build();

        // 새로운 유저의 정보 생성
        UserDetail newUserDetail = UserDetail.builder()
                .user(newUser)
                .employeeNumber(employeeNumber)
                .name(name)
                .joinDate(joinDate)
                .phoneNumber("") // 기본값, 실제로는 사용자로부터 전화번호를 받아야 합니다.
                .zipCode("") // 기본값, 실제로는 사용자로부터 우편번호를 받아야 합니다.
                .address("") // 기본값, 실제로는 사용자로부터 주소를 받아야 합니다.
                .detailAddress("") // 기본값, 실제로는 사용자로부터 상세 주소를 받아야 합니다.
                .build();

        // 연관 관계 편의 메소드로 user와 userDetail을 서로 참조하도록 설정
        newUser.assignUserDetail(newUserDetail);

        // 사용자 및 정보 저장 후 생성 (사용자 식별자를 얻기 위해 미리 DB에 저장)
        // Cascade 설정으로 UserDetail도 함께 저장된다.
        userRepository.save(newUser);

        // 사용자의 마일리지 생성
        mileageFeignClient.createMileage(newUserDetail.getUser().getId(), newUserDetail.getUser().getId());

        log.info("사용자 생성 완료. 사용자 ID: {}", newUser.getId());
    }

    @Override
    public void deleteAccount(Long userId, String oldPassword, WithdrawalReason reason, String message) {

        // 식별자로 계정 조회. 계정이 존재하지 않으면 예외 발생
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 유저 탈퇴 사유 생성
        UserWithdrawal userWithdrawal = UserWithdrawal
                .builder()
                .reason(reason)
                .message(message)
                .withdrawalDate(LocalDate.now())
                .build();

        // 계정, 정보, 장바구니, 위시리스트 삭제
        userRepository.delete(user);
        userDetailRepository.delete(user.getUserDetail());
        cartRepository.delete(cartRepository.findCartByUserId(userId));
        wishlistRepository.delete(wishlistRepository.findWishlistByUserId(userId));

        // 마일리지 삭제
        mileageFeignClient.deleteMileage(userId, userId);

        log.info("탈퇴 성공 후 탈퇴 사유 생성 성공. 탈퇴 ID: {}", userWithdrawal.getId());
    }

    // 사용자 정보 조회
    @Override
    public UserDetail getUserById(Long userId) {
        log.info("사용자 정보 조회 성공. 사용자 아이디: {}", userId);
        return userDetailRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    // 사용자 주소 변경
    public void updateAddress(Long userId, String phoneNumber, String zipCode, String address, String detailAddress) {
        // 사용자 정보 찾기
        UserDetail userDetail = userDetailRepository.findByUserId(userId);
        // 엔티티 변경
        userDetail.changeAddress(phoneNumber, zipCode, address, detailAddress);
        // 변경된 내용을 데이터베이스에 반영
        userDetailRepository.save(userDetail);
        log.info("사용자 주소 변경 성공. 사용자 ID: {}", userDetail.getUser().getId());
    }

    // ==========================관리자===========================

    // 관리자의 사용자 정보 조회
    @Override
    public UserDetail getAdminUserById(Long userId) {
        log.info("사용자 정보 조회 성공. 사용자 아이디: {}", userId);
        return userDetailRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    // 관리자의 사용자 정보 리스트 조회
    @Override
    public List<AdminReadUserListResponseDto> getUserList(Category category, String keyword, Pageable pageable) {

        // 키워드가 있는 경우
        if(keyword != null) {
            switch (category) {
                case EMPLOYEENUMBER -> {
                    if(keyword.matches("\\d+")) {
                        Long employeeNumber = Long.parseLong(keyword);
                        Page<UserDetail> userIds = userDetailRepository.findByEmployeeNumber(employeeNumber, pageable);
                        log.info("키워드 조건에 따른 사용자 정보 조회 성공. 조건: {}. 키워드: {}.", category, keyword);
                        return userIds.stream()
                                .map(AdminReadUserListResponseDto::new)
                                .collect(Collectors.toList());
                    }
                    else {
                        return null;
                    }
                }
                case EMAIL -> {
                    Page<User> userIds = userRepository.findByEmailContaining(keyword, pageable);
                    log.info("조건에 따른 사용자 정보 조회 성공. 조건: {}", keyword);
                    return userIds.stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                }
                case NAME -> {
                    Page<UserDetail> userIds = userDetailRepository.findByNameContaining(keyword, pageable);
                    log.info("키워드 조건에 따른 사용자 정보 조회 성공. 조건: {}. 키워드: {}.", category, keyword);
                    return userIds.stream()
                            .map(AdminReadUserListResponseDto::new)
                            .collect(Collectors.toList());
                }
                case ALL -> {
                    Page<UserDetail> userIds = userDetailRepository.findAll(pageable);
                    log.info("사용자 전체 정보 조회 성공.");
                    return userIds.stream()
                            .map(AdminReadUserListResponseDto::new)
                            .collect(Collectors.toList());
                }
            }
        }
        else {
            Page<UserDetail> userIds = userDetailRepository.findAll(pageable);
            log.info("사용자 전체 정보 조회 성공.");
            return userIds.stream()
                    .map(AdminReadUserListResponseDto::new)
                    .collect(Collectors.toList());
        }
        Page<UserDetail> userIds = userDetailRepository.findAll(pageable);
        log.info("사용자 전체 정보 조회 성공.");
        return userIds.stream()
                .map(AdminReadUserListResponseDto::new)
                .collect(Collectors.toList());
    }

    private AdminReadUserListResponseDto convertToDto(User user) {
        return new AdminReadUserListResponseDto(user.getUserDetail());
    }

    // 관리자의 사용자 정보 삭제
    @Override
    public void deleteUser(List<Long> userIds) {

        // 사용자 목록 별로 조회
        for (Long userId : userIds) {
            // 사용자 조회, 없을 경우 예외 발생
            User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

            // 사용자 삭제
            userRepository.delete(user);

            log.info("사용자 정보 삭제 성공. 삭제된 사용자 아이디: {}", userIds);
        }
    }

    //==============================Feign요청=======================

    // 상품 서비스에서의 리뷰자 이름 요청
    @Override
    public User getReviewer(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    // QNA 서비스에서의 작성자 이름 및 이메일 요청
    @Override
    public QnaAuthorDto getQnaAuthor(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new QnaAuthorDto(user.getUserDetail().getName(), user.getEmail());
    }

    // 인증 서비스에서의 사용자 리스트 요청
    public List<UserDetail> getUserList(List<Long> userIds) {
        return userDetailRepository.findAllByIdIn(userIds);
    }
}
