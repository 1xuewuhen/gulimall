package com.xwh.gulimall.order.vo;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo  {

    @Setter
    @Getter
    private List<MemberAddressVo> address;

    @Setter
    @Getter
    private List<OrderItemVo> items;

    @Setter
    @Getter
    private Integer integration;

    @Setter
    @Getter
    private Map<Long, Boolean> stocks;

    @Getter
    @Setter
    private String orderToken;

    public Integer getCount() {
        Integer i = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

//    @Setter
//    private BigDecimal total;

//    @Setter
//    @Getter
//    private BigDecimal pauPrice;

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }
        return sum;
    }

    public BigDecimal getPayPrice() {
        BigDecimal sum = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                sum = sum.add(item.getPrice().multiply(new BigDecimal(item.getCount().toString())));
            }
        }
        return sum;
    }

}