/**
 * Copyright 2022 bejson.com
 */
package com.xwh.gulimall.member.vo;

import lombok.Data;

/**
 * Auto-generated: 2022-12-11 10:32:7
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */

@Data
public class SocialUser {

    private String access_token;
    private String token_type;
    private long expires_in;
    private String refresh_token;
    private String scope;
    private long created_at;
    private String Uid;
}