package com.yupi.yuoj.judge.codesandbox.model;

import lombok.Data;

/**
 * 单个用例判题结果
 */
@Data
public class JudgeCaseResult {

    /**
     * 用例序号（从 1 开始）
     */
    private Integer index;

    /**
     * 用例状态（如 AC / WA）
     */
    private String status;
}
