package com.xwh.common.exception;

public class NoStockException extends RuntimeException {
    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品id:" + skuId + "没有足够的库存了");
    }

    public NoStockException(String message) {
        super(message);
    }

    public Long getSkuId() {
        return skuId;
    }
}
