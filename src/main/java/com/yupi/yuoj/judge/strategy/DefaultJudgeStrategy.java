package com.yupi.yuoj.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.yupi.yuoj.judge.codesandbox.model.JudgeCaseResult;
import com.yupi.yuoj.judge.codesandbox.model.JudgeInfo;
import com.yupi.yuoj.model.dto.question.JudgeCase;
import com.yupi.yuoj.model.dto.question.JudgeConfig;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.enums.JudgeInfoMessageEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 默认判题策略
 */
public class DefaultJudgeStrategy implements JudgeStrategy {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        if (outputList == null) {
            outputList = Collections.emptyList();
        }
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        if (judgeCaseList == null) {
            judgeCaseList = Collections.emptyList();
        }

        JudgeInfoMessageEnum finalResult = buildFinalResult(judgeCaseList, inputList, outputList, question, memory, time);
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);
        judgeInfoResponse.setCaseResults(buildCaseResults(judgeCaseList, outputList, finalResult));
        judgeInfoResponse.setMessage(finalResult.getValue());
        return judgeInfoResponse;
    }

    private JudgeInfoMessageEnum buildFinalResult(List<JudgeCase> judgeCaseList, List<String> inputList,
            List<String> outputList, Question question, Long memory, Long time) {
        // 判题限制（优先于输出比较）
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long needMemoryLimit = judgeConfig.getMemoryLimit();
        Long needTimeLimit = judgeConfig.getTimeLimit();
        if (memory != null && needMemoryLimit != null && memory > needMemoryLimit) {
            return JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
        }
        if (time != null && needTimeLimit != null && time > needTimeLimit) {
            return JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
        }
        // 输出数量不一致
        if (outputList.size() != inputList.size()) {
            return JudgeInfoMessageEnum.WRONG_ANSWER;
        }
        // 逐个比较输出
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expected = normalizeOutput(judgeCase.getOutput());
            String actual = normalizeOutput(outputList.get(i));
            if (!expected.equals(actual)) {
                return JudgeInfoMessageEnum.WRONG_ANSWER;
            }
        }
        return JudgeInfoMessageEnum.ACCEPTED;
    }

    private List<JudgeCaseResult> buildCaseResults(List<JudgeCase> judgeCaseList, List<String> outputList,
            JudgeInfoMessageEnum finalResult) {
        List<JudgeCaseResult> results = new ArrayList<>();
        if (finalResult == JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED) {
            for (int i = 0; i < judgeCaseList.size(); i++) {
                JudgeCaseResult result = new JudgeCaseResult();
                result.setIndex(i + 1);
                result.setStatus("TLE");
                results.add(result);
            }
            return results;
        }
        if (finalResult == JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED) {
            for (int i = 0; i < judgeCaseList.size(); i++) {
                JudgeCaseResult result = new JudgeCaseResult();
                result.setIndex(i + 1);
                result.setStatus("MLE");
                results.add(result);
            }
            return results;
        }
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase = judgeCaseList.get(i);
            String expected = normalizeOutput(judgeCase == null ? null : judgeCase.getOutput());
            String actual = normalizeOutput(i < outputList.size() ? outputList.get(i) : null);
            JudgeCaseResult result = new JudgeCaseResult();
            result.setIndex(i + 1);
            result.setStatus(expected.equals(actual) ? "AC" : "WA");
            results.add(result);
        }
        return results;
    }

    private String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        int end = lines.length;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = rtrimLine(lines[i]);
        }
        while (end > 0 && lines[end - 1].isEmpty()) {
            end--;
        }
        if (end == 0) {
            return "";
        }
        return String.join("\n", java.util.Arrays.copyOf(lines, end));
    }

    private String rtrimLine(String line) {
        int end = line.length();
        while (end > 0) {
            char ch = line.charAt(end - 1);
            if (ch == ' ' || ch == '\t') {
                end--;
            } else {
                break;
            }
        }
        return line.substring(0, end);
    }
}
