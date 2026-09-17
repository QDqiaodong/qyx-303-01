package com.example.tuanjian.exception;

/**
 * 落地成团相关的业务规则冲突：余额不足、场地同日已被占、
 * 方案/人数/天数/活动不满足当次对比口径、批次已失效等。
 * 默认按 409 Conflict 返回。
 */
public class BusinessConflictException extends RuntimeException {

    public BusinessConflictException(String message) {
        super(message);
    }

}
