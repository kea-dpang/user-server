package com.example.shoppingmallserver.dto.response.user;

import com.example.shoppingmallserver.entity.user.UserDetail;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 관리자가 조회하는 사용자 정보 목록을 담는 DTO 클래스입니다.
 * 사용자 ID, 사원 번호, 이름, 이메일, 입사일을 포함합니다.
 */
@Getter
public class AdminReadUserListResponseDto {
    private final Long userId;
    private final Long employeeNumber;
    private final String name;
    private final String email;
    private final LocalDate joinDate;

    /**
     * UserDetail 엔티티를 이용하여 AdminReadUserDto를 생성합니다.
     *
     * @param userDetail 관리자가 조회하는 사용자의 상세 정보를 담은 엔티티
     */
    public AdminReadUserListResponseDto(UserDetail userDetail) {
        this.userId = userDetail.getUser().getId();
        this.employeeNumber = userDetail.getEmployeeNumber();
        this.name = userDetail.getName();
        this.email = userDetail.getUser().getEmail();
        this.joinDate = userDetail.getJoinDate();
    }
}