package com.xwh.gulimall.order.vo;


import com.xwh.gulimall.order.entity.OrderEntity;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubmitOrderResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private OrderEntity order;
    private Integer code;
}
