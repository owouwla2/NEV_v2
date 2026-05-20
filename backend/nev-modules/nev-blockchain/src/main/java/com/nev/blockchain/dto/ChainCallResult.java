package com.nev.blockchain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 链调用统一结果封装
 *
 * 设计目的：屏蔽 WeBASE-Front 返回格式的版本差异，业务层只关心 success/txHash/blockNumber
 *
 * @author NEV-v2
 */
@Data
@Builder
public class ChainCallResult {

    /** 是否成功（按 WeBASE-Front 的 code/statusOK/error 字段宽松判定） */
    private boolean success;

    /** 交易哈希（写链调用才有；只读 query 调用为 null） */
    private String txHash;

    /** 区块高度（写链调用才有） */
    private Long blockNumber;

    /** 链上返回的可读消息（错误描述或方法返回值文本） */
    private String message;

    /** 链上返回值（视图函数 query 调用的返回数据，原始结构） */
    private Object output;

    /** WeBASE-Front 原始响应（调试用，建议日志保留） */
    private String rawResponse;
}
