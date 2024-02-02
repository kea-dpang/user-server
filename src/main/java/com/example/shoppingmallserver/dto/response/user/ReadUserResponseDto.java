package com.example.shoppingmallserver.dto.response.user;

import com.example.shoppingmallserver.entity.user.UserDetail;

import lombok.Getter;

import java.time.LocalDate;

/**
 * 사용자 정보를 조회하기 위한 DTO 클래스입니다.
 * 사원 번호, 이름, 이메일, 가입일 정보를 포함합니다.
 */
@Getter
public class ReadUserResponseDto {
    private final Long userId;
    private final Long employeeNumber;
    private final String name;
    private final String email;
    private final LocalDate joinDate;

    /**
     * UserDetail 엔티티를 이용하여 ReadUserDto를 생성합니다.
     *
     * @param userDetail 조회하는 사용자의 상세 정보를 담은 엔티티
     */
    public ReadUserResponseDto(UserDetail userDetail) {
        this.userId = userDetail.getUser().getId();
        this.employeeNumber = userDetail.getEmployeeNumber();
        this.name = userDetail.getName();
        this.email = userDetail.getUser().getEmail();
        this.joinDate = userDetail.getJoinDate();
    }
}