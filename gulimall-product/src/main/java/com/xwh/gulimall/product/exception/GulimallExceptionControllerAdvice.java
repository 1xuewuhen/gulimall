package com.xwh.gulimall.product.exception;


import com.xwh.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * if (result.hasErrors()) {
 * Map<String,String> map = new HashMap<>();
 * List<FieldError> fieldErrors = result.getFieldErrors();
 * fieldErrors.forEach(fieldError -> {
 * String message = fieldError.getDefaultMessage();
 * String field = fieldError.getField();
 * map.put(field,message);
 * });
 * return R.error(400,"提交的数据不合法").put("data",map);
 * } else {
 * }
 */
@Slf4j
@RestControllerAdvice(basePackages = {"com.xwh.gulimall.product.controller"})
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(value = {
            MethodArgumentNotValidException.class
    })
    public R handleVaildException(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题:{},异常类型:{}", e.getMessage(), e.getClass());
        BindingResult result = e.getBindingResult();

        Map<String, String> map = new HashMap<>();
        List<FieldError> fieldErrors = result.getFieldErrors();
        fieldErrors.forEach(fieldError -> {
            String message = fieldError.getDefaultMessage();
            String field = fieldError.getField();
            map.put(field, message);
        });
        return R.error(400, "提交的数据不合法").put("data", map);


    }

}
