package com.example.shoppingmallserver.dto.cart_wishlist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemCartInquiryDto {
    private Long itemId; // 상품 ID
    private String image; // 상품 이미지 URL
    private String name; // 상품 이름
    private int price; // 상품 정가
    private int discountRate; // 할인율
    private int discountPrice; // 상품 판매가
}
