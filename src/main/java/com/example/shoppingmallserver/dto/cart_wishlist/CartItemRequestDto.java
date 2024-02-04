package com.example.shoppingmallserver.dto.cart_wishlist;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CartItemRequestDto {
    private Long itemId;
    private int quantity;
}
