package com.yupi.yuoj.judge;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yupi.yuoj.common.ErrorCode;
import com.yupi.yuoj.exception.BusinessException;
import com.yupi.yuoj.judge.codesandbox.CodeSandbox;
import com.yupi.yuoj.judge.codesandbox.CodeSandboxFactory;
import com.yupi.yuoj.judge.codesandbox.CodeSandboxProxy;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.yupi.yuoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.yupi.yuoj.judge.codesandbox.model.JudgeInfo;
import com.yupi.yuoj.judge.strategy.JudgeContext;
import com.yupi.yuoj.model.dto.question.JudgeCase;
import com.yupi.yuoj.model.entity.Question;
import com.yupi.yuoj.model.entity.QuestionSubmit;
import com.yupi.yuoj.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuoj.model.enums.QuestionSubmitStatusEnum;
import com.yupi.yuoj.service.QuestionService;
import com.yupi.yuoj.service.QuestionSubmitService;
import com.yupi.yuoj.utils.MemoryUnitUtil;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private QuestionService questionService;

    @Value("${codesandbox.type:example}")
    private String type;

    @Resource
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1) 获取题目提交与题目信息
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2) 非等待状态拒绝重复判题
        if (!QuestionSubmitStatusEnum.WAITING.getValue().equals(questionSubmit.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }

        // 3) 更新状态为“判题中”（幂等）
        QuestionSubmit runningUpdate = new QuestionSubmit();
        runningUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        LambdaUpdateWrapper<QuestionSubmit> runningWrapper = new LambdaUpdateWrapper<>();
        runningWrapper.eq(QuestionSubmit::getId, questionSubmitId)
                .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.WAITING.getValue())
                .eq(QuestionSubmit::getIsDelete, 0);
        boolean update = questionSubmitService.update(runningUpdate, runningWrapper);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交状态已变化，无法判题");
        }

        try {
            // 4) 调用沙箱执行
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            String language = questionSubmit.getLanguage();
            String code = questionSubmit.getCode();
            String judgeCaseStr = question.getJudgeCase();
            List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
            List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .language(language)
                    .inputList(inputList)
                    .build();
            ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
            if (executeCodeResponse == null || executeCodeResponse.getJudgeInfo() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "沙箱响应异常");
            }
            normalizeMemoryUnit(executeCodeResponse);

            // 5) 判题策略生成结果
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
            judgeContext.setInputList(inputList);
            judgeContext.setOutputList(executeCodeResponse.getOutputList());
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);

            // 6) 落库判题结果（幂等）
            String message = judgeInfo.getMessage();
            JudgeInfoMessageEnum messageEnum = getJudgeInfoMessageEnum(message);
            String result = getResultByMessageEnum(messageEnum);
            boolean accepted = JudgeInfoMessageEnum.ACCEPTED.equals(messageEnum);
            QuestionSubmit finishUpdate = new QuestionSubmit();
            finishUpdate.setStatus(accepted ? QuestionSubmitStatusEnum.SUCCEED.getValue()
                    : QuestionSubmitStatusEnum.FAILED.getValue());
            finishUpdate.setResult(result);
            finishUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            LambdaUpdateWrapper<QuestionSubmit> finishWrapper = new LambdaUpdateWrapper<>();
            finishWrapper.eq(QuestionSubmit::getId, questionSubmitId)
                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                    .eq(QuestionSubmit::getIsDelete, 0);
            update = questionSubmitService.update(finishUpdate, finishWrapper);
            if (!update) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "判题结果更新失败");
            }

            if (accepted) {
                boolean acceptedUpdate = questionService.update()
                        .eq("id", questionId)
                        .setSql("acceptedNum = IFNULL(acceptedNum, 0) + 1")
                        .update();
                if (!acceptedUpdate) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "通过数更新失败");
                }
            }
            return questionSubmitService.getById(questionSubmitId);
        } catch (Exception e) {
            log.error("判题执行异常，提交id={}", questionSubmitId, e);
            JudgeInfo errorInfo = new JudgeInfo();
            errorInfo.setMessage("系统异常");
            QuestionSubmit failUpdate = new QuestionSubmit();
            failUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            failUpdate.setResult("SE");//System Error
            failUpdate.setJudgeInfo(JSONUtil.toJsonStr(errorInfo));
            LambdaUpdateWrapper<QuestionSubmit> failWrapper = new LambdaUpdateWrapper<>();
            failWrapper.eq(QuestionSubmit::getId, questionSubmitId)
                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                    .eq(QuestionSubmit::getIsDelete, 0);
            questionSubmitService.update(failUpdate, failWrapper);
            throw e;
        }
    }

    private void normalizeMemoryUnit(ExecuteCodeResponse executeCodeResponse) {
        JudgeInfo judgeInfo = executeCodeResponse.getJudgeInfo();
        if (judgeInfo == null) {
            return;
        }
        Long memory = judgeInfo.getMemory();
        if (memory == null || memory <= 0) {
            return;
        }
        if ("remote".equalsIgnoreCase(type)) {
            judgeInfo.setMemory(MemoryUnitUtil.bytesToKb(memory));
        }
    }

    private JudgeInfoMessageEnum getJudgeInfoMessageEnum(String message) {
        if (StringUtils.isBlank(message)) {
            return JudgeInfoMessageEnum.SYSTEM_ERROR;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (message.equals(anEnum.getValue()) || message.equals(anEnum.getText())) {
                return anEnum;
            }
        }
        return JudgeInfoMessageEnum.SYSTEM_ERROR;
    }

    private String getResultByMessageEnum(JudgeInfoMessageEnum messageEnum) {
        if (messageEnum == null) {
            return "SE";
        }
        switch (messageEnum) {
            case ACCEPTED:
                return "AC";
            case WRONG_ANSWER:
                return "WA";
            case TIME_LIMIT_EXCEEDED:
                return "TLE";
            case MEMORY_LIMIT_EXCEEDED:
                return "MLE";
            case RUNTIME_ERROR:
                return "RE";
            case COMPILE_ERROR:
                return "CE";
            case PRESENTATION_ERROR:
                return "PE";
            case OUTPUT_LIMIT_EXCEEDED:
                return "OLE";
            case DANGEROUS_OPERATION:
                return "DANGEROUS";
            case WAITING:
                return "WAITING";
            default:
                return "SE";
        }
    }
}
