package com.example.shoppingmallserver.controller;

import com.example.shoppingmallserver.base.BaseResponse;
import com.example.shoppingmallserver.base.SuccessResponse;
import com.example.shoppingmallserver.dto.request.user.AddressRequestDto;
import com.example.shoppingmallserver.dto.request.user.RegisterRequestDto;
import com.example.shoppingmallserver.dto.request.user.WithdrawalRequestDto;
import com.example.shoppingmallserver.dto.response.user.AdminReadUserListResponseDto;
import com.example.shoppingmallserver.dto.response.user.AdminReadUserResponseDto;
import com.example.shoppingmallserver.dto.request.user.DeleteListRequestDto;
import com.example.shoppingmallserver.dto.response.user.ReadUserAddressResponseDto;
import com.example.shoppingmallserver.dto.response.user.ReadUserResponseDto;
import com.example.shoppingmallserver.entity.user.User;
import com.example.shoppingmallserver.entity.user.UserDetail;
import com.example.shoppingmallserver.service.Category;
import com.example.shoppingmallserver.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 정보를 관리하는 Controller 클래스입니다.
 * 사용자 정보 조회, 생성, 관리자에 의한 조회 및 삭제 기능을 제공합니다.
 */
@Tag(name = "User", description = "User 서비스 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자를 등록하는 API입니다.
     *
     * @param requestDto 사용자 등록 요청 정보
     * @return HTTP 상태 코드 201 (CREATED)
     */
    @PostMapping("/register")
    @Operation(summary = "사용자 등록", description = "사용자를 생성합니다.", hidden = true)
    public ResponseEntity<BaseResponse> register(@RequestBody @Parameter(description = "사용자 등록 정보") RegisterRequestDto requestDto) {

        // 사용자 등록
        userService.register(
                requestDto.getEmail(),
                requestDto.getEmployeeNumber(),
                requestDto.getName(),
                requestDto.getJoinDate()
        );

        // 성공 응답 반환
        return new ResponseEntity<>(
                new BaseResponse(HttpStatus.CREATED.value(), "사용자가 성공적으로 등록되었습니다."),
                HttpStatus.CREATED
        );
    }

    /**
     * 클라이언트가 제공한 사용자ID를 이용하여 계정을 삭제하고, 성공 메시지를 반환합니다.
     * 계정 삭제가 실패하면 UserNotFoundException이 발생하게 됩니다.
     *
     * @param id 삭제할 계정의 식별자
     * @return 성공 응답을 포함한 ResponseEntity. 계정 삭제가 성공하면 200 상태 코드와 성공 메시지를 반환
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 탈퇴", description = "사용자가 탈퇴를 했을 때, 정보를 삭제합니다.")
    public ResponseEntity<BaseResponse> deleteAccount(@PathVariable @Parameter(description = "사용자 ID(PK)", example = "1") Long id, @RequestBody WithdrawalRequestDto withdrawalRequestDto) {

        // 사용자 ID로 계정 삭제
        userService.deleteAccount(id, withdrawalRequestDto.getReason(), withdrawalRequestDto.getMessage());

        // 성공 응답 생성 및 반환
        return new ResponseEntity<>(
                new BaseResponse(HttpStatus.OK.value(), "계정이 삭제되었습니다."),
                HttpStatus.OK
        );
    }

    /**
     * 사용자 ID를 기반으로 사용자의 상세 정보를 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 성공 응답 메시지와 함께 조회한 사용자 정보를 담은 DTO를 반환
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 상세 정보 조회", description = "사용자의 상세 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<ReadUserResponseDto>> getUser(@PathVariable @Parameter(description = "사용자 ID(PK)", example = "1") Long userId) {

        // 사용자 ID를 기반으로 사용자의 상세 정보를 조회
        UserDetail userDetail = userService.getUserById(userId);

        // 조회한 사용자 정보를 이용하여 응답 DTO를 생성
        ReadUserResponseDto data = new ReadUserResponseDto(userDetail);

        // 생성한 응답 DTO를 포함하는 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 정보가 성공적으로 조회되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자 정보를 성공적으로 조회하였습니다.", data),
                HttpStatus.OK
        );

    }

    @GetMapping("/{userId}/address")
    @Operation(summary = "사용자 주소 조회", description = "사용자가 입력한 주소를 조회합니다.")
    public ResponseEntity<SuccessResponse<ReadUserAddressResponseDto>> getAddress(@PathVariable @Parameter(description = "사용자 ID(PK)", example = "1") Long userId) {

        // 사용자 ID를 기반으로 사용자의 상세 정보를 조회
        UserDetail userDetail = userService.getUserById(userId);

        // 조회한 사용자 정보를 이용하여 응답 DTO를 생성
        ReadUserAddressResponseDto data = new ReadUserAddressResponseDto(userDetail);

        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자의 주소를 성공적으로 조회하였습니다.", data),
                HttpStatus.OK
        );
    }

    /**
     * 사용자 ID를 기반으로 사용자의 주소를 변경합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 성공 응답 메시지와 204코드 반환
     */
    @PatchMapping("/{userId}/address")
    @Operation(summary = "사용자 주소 변경", description = "사용자가 입력한 주소로 변경합니다.")
    public ResponseEntity<SuccessResponse<Void>> updateAddress(@PathVariable @Parameter(description = "사용자 ID(PK)", example = "1") Long userId, @RequestBody @Parameter(description = "사용자가 입력한 주소") AddressRequestDto addressRequestDto) {

        // 받은 정보를 통해 주소지 변경
        userService.updateAddress(userId, addressRequestDto.getPhoneNumber(), addressRequestDto.getZipCode(), addressRequestDto.getAddress(), addressRequestDto.getDetailAddress());

        //  변경 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 주소가 성공적으로 변경되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.NO_CONTENT.value(), "사용자의 주소를 성공적으로 변경하였습니다.", null),
                HttpStatus.NO_CONTENT
        );
    }

    // ==========================관리자===========================

    /**
     * 관리자가 사용자 ID를 기반으로 사용자의 상세 정보를 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 성공 응답 메시지와 함께 조회한 사용자 정보를 담은 DTO를 반환
     */
    @GetMapping("/{userId}/temp") // AI에 물어봐서 고치기 // @PreAuthorize -> 찾아보기
    @Operation(summary = "(관리자) 사용자 상세 정보 조회", description = "관리자가 사용자 상세 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<AdminReadUserResponseDto>> adminGetUser(@PathVariable @Parameter(description = "사용자 ID(PK)", example = "1") Long userId) {

        // 사용자 ID를 기반으로 사용자의 상세 정보를 조회
        UserDetail userDetail = userService.getAdminUserById(userId);
        User user = userDetail.getUser();

        // 조회한 사용자 정보를 이용하여 응답 DTO를 생성
        AdminReadUserResponseDto data = new AdminReadUserResponseDto(user, userDetail);

        // 생성한 응답 DTO를 포함하는 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 정보가 성공적으로 조회되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자 정보를 성공적으로 조회하였습니다.", data),
                HttpStatus.OK
        );

    }

    /**
     * 관리자가 키워드를 기반으로 사용자 정보 목록을 조회합니다.
     *
     * @param category 사용자 정보에서 검색할 키워드
     * @return 성공 응답 메시지와 함께 조회한 사용자 정보 목록을 담은 DTO 목록을 반환
     */
    @GetMapping("/find")
    @Operation(summary = "(관리자) 사용자 정보 목록 조회", description = "관리자가 사용자 정보 목록을 조회합니다.")
    public ResponseEntity<SuccessResponse<Page<AdminReadUserListResponseDto>>> adminGetUserList(@RequestParam @Parameter(description = "(관리자) 사용자 검색 카테고리", example = "NULL") Category category, @RequestParam(required = false) @Parameter(description = "(관리자) 사용자 검색 키워드", example = "김디팡") String keyword, Pageable pageable) {

        // 키워드를 기반으로 사용자 정보 목록을 조회 (관리자용)
        Page<AdminReadUserListResponseDto> userDetails = userService.getUserList(category, keyword, pageable);

        // 생성한 응답 DTO 목록을 포함하는 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 정보 목록이 성공적으로 조회되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자 정보를 성공적으로 조회하였습니다.", userDetails),
                HttpStatus.OK
        );
    }

    /**
     * 관리자가 요청 본문으로 받은 사용자 ID 목록을 이용하여 해당 사용자들을 삭제합니다.
     *
     * @param deleteListRequestDto 삭제할 사용자의 ID 리스트
     * @return 성공 응답 메시지와 함께 삭제된 사용자의 ID 목록을 반환
     */
    @DeleteMapping("/list")// 이거는 일단 냅둬
    @Operation(summary = "(관리자) 사용자 삭제", description = "관리자가 사용자를 삭제합니다.")
    public ResponseEntity<SuccessResponse<String>> adminDeleteUser(@RequestBody @Parameter(description = "사용자 ID(PK) 목록") DeleteListRequestDto deleteListRequestDto) {

        // 요청 본문으로 받은 사용자 ID 목록을 이용하여 해당 사용자들을 삭제
        userService.deleteUser(deleteListRequestDto.getUserIds());

        // 삭제한 사용자 ID 목록을 포함하는 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 정보가 성공적으로 삭제되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자 정보를 성공적으로 삭제하였습니다.", "Deleted user IDs: " + deleteListRequestDto.getUserIds()),
                HttpStatus.OK
        );
    }

    //==============================Feign요청=======================

    @GetMapping("/list")
    @Operation(summary = "(백엔드) 사용자 상세 정보 리스트로 조회", description = "백엔드에서 사용자 상세 정보 리스트를 조회합니다.")
    public ResponseEntity<SuccessResponse<List<AdminReadUserListResponseDto>>> getUsersInfo(@RequestParam List<Long> userIds) {

        // Auth 서비스에서 이쪽으로 전해줄 DTO를 받아서 유저 아이디 리스트로 유저 정보 리스트를 요청
        List<UserDetail> userDetails = userService.getUserList(userIds);

        // DTO 리스트 생성
        List<AdminReadUserListResponseDto> data = userDetails.stream().map(AdminReadUserListResponseDto::new).toList();

        // 생성한 응답 DTO 목록을 포함하는 성공 응답 메시지를 생성하고, 이를 ResponseEntity로 감싸어 반환
        // 이를 통해 API 호출한 클라이언트에게 사용자 정보 목록이 성공적으로 조회되었음을 알림
        return new ResponseEntity<>(
                new SuccessResponse<>(HttpStatus.OK.value(), "사용자 정보를 성공적으로 조회하였습니다.", data),
                HttpStatus.OK
        );

    }

}
