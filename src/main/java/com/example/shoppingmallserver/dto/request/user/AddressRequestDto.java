package com.example.shoppingmallserver.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "주소 정보")
@Getter
public class AddressRequestDto {
    @Schema(description = "전화번호", example = "010-1234-5678")
    private final String phoneNumber;
    @Schema(description = "우편번호", example = "461831")
    private final String zipCode;
    @Schema(description = "주소", example = "경기 성남시 수정구 복호동 495")
    private final String address;
    @Schema(description = "상세주소", example = "AI공학관 411")
    private final String detailAddress;

    public AddressRequestDto(String phoneNumber, String zipCode, String address, String detailAddress) {
        this.phoneNumber = phoneNumber;
        this.zipCode = zipCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }
}
