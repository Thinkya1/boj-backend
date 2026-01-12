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
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
        // 1) 传入题目提交 id，获取对应的题目与提交信息（含代码、语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2) 提交状态不是等待中则直接拒绝，避免重复判题
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
            // 4) 调用沙箱获取执行结果
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            String language = questionSubmit.getLanguage();
            String code = questionSubmit.getCode();
            // 获取输入用例
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
            List<String> outputList = executeCodeResponse.getOutputList();
            // 5) 生成判题信息
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
            judgeContext.setInputList(inputList);
            judgeContext.setOutputList(outputList);
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
            // 6) 落库判题结果（幂等）
            QuestionSubmit finishUpdate = new QuestionSubmit();
            finishUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            finishUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            LambdaUpdateWrapper<QuestionSubmit> finishWrapper = new LambdaUpdateWrapper<>();
            finishWrapper.eq(QuestionSubmit::getId, questionSubmitId)
                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                    .eq(QuestionSubmit::getIsDelete, 0);
            update = questionSubmitService.update(finishUpdate, finishWrapper);
            if (!update) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "判题结果更新失败");
            }
            String message = judgeInfo.getMessage();
            boolean accepted = message != null && (message.equals(JudgeInfoMessageEnum.ACCEPTED.getValue())
                    || message.equals(JudgeInfoMessageEnum.ACCEPTED.getText()));
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
            //设置题目状态
            failUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            failUpdate.setJudgeInfo(JSONUtil.toJsonStr(errorInfo));
            LambdaUpdateWrapper<QuestionSubmit> failWrapper = new LambdaUpdateWrapper<>();
            failWrapper.eq(QuestionSubmit::getId, questionSubmitId)
                    //防止重复更新 / 状态回滚 例如 成功后的记录又改回失败
                    .eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.RUNNING.getValue())
                    .eq(QuestionSubmit::getIsDelete, 0);
            questionSubmitService.update(failUpdate, failWrapper);
            throw e;
        }
    }
}
