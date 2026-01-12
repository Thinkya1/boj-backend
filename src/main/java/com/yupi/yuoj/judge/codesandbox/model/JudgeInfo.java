package com.yupi.yuoj.judge.codesandbox.model;

import java.util.List;
import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {

    /**
     * 判题消息，例如：Accepted / Wrong Answer / Time Limit Exceeded / Memory Limit Exceeded / Runtime Error 等
     */
    private String message;

    /**
     * 内存占用（KB）
     */
    private Long memory;

    /**
     * 耗时（ms）
     */
    private Long time;

    /**
     * 单个测试点结果
     */
    private List<JudgeCaseResult> caseResults;
}
