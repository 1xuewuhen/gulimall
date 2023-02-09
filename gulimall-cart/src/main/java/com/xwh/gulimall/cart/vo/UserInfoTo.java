package com.xwh.gulimall.cart.vo;


import lombok.Data;

@Data
public class UserInfoTo {

    private Long userId;
    private String userKey;
    private Boolean tempUser = false;
}
