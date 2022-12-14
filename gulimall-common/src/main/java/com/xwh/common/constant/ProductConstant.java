package com.xwh.common.constant;

public class ProductConstant {

    public enum AttrEnum{
        ATTR_TYPE_BASE(1,"基本属性"),ATTR_TYPE_SALE(0,"销售属性");
        private Integer code;
        private String message;

        AttrEnum(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    public enum StatusEnum{
        NEW_SPU(0,"新建"),SPU_UP(1,"商品上架"),SPU_DOWN(2,"商品下架");
        private Integer code;
        private String message;

        StatusEnum(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
