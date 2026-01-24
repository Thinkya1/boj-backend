package biny.biny.controller;

import biny.biny.common.BaseResponse;
import biny.biny.common.ErrorCode;
import biny.biny.common.ResultUtils;
import biny.biny.exception.BusinessException;
import biny.biny.judge.JudgeService;
import biny.biny.judge.codesandbox.model.ExecuteCodeResponse;
import biny.biny.model.dto.questionsubmit.QuestionSubmitAddRequest;
import biny.biny.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 题目运行接口
 *
 * @author biny
 */
@RestController
@RequestMapping("/question_run")
@Slf4j
public class QuestionRunController {

    private final JudgeService judgeService;

    private final UserService userService;

    public QuestionRunController(JudgeService judgeService, UserService userService) {
        this.judgeService = judgeService;
        this.userService = userService;
    }

    /**
     * 运行示例用例
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return
     */
    @PostMapping("/")
    public BaseResponse<ExecuteCodeResponse> runQuestion(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                                         HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() == null
                || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        userService.getLoginUser(request);
        ExecuteCodeResponse executeCodeResponse = judgeService.runQuestion(questionSubmitAddRequest);
        return ResultUtils.success(executeCodeResponse);
    }
}
