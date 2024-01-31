package com.example.shoppingmallserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QnaAuthorDto {
    private final String name;
    private final String email;
}
