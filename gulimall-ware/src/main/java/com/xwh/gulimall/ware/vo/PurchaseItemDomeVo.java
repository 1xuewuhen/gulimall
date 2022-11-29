package com.xwh.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDomeVo {
    /**
     * items: [{itemId:1,status:4,reason:""}]
     */
    private Long itemId;
    private Integer status;
    private String reason;

}
