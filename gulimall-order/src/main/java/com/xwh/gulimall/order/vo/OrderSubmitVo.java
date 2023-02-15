package com.xwh.gulimall.order.vo;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId;
    private Integer payType;
    //无需获取购物车中的数据，从数据库中在取一遍
    //优惠，发票
    private String orderToken;//防重令牌
    private BigDecimal payPrice;
    private String note;

}
