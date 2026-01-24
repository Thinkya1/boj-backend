package biny.biny.judge;

import biny.biny.model.entity.QuestionSubmit;
import biny.biny.judge.codesandbox.model.ExecuteCodeResponse;
import biny.biny.model.dto.questionsubmit.QuestionSubmitAddRequest;

/**
 * 判题服务
 */
public interface JudgeService {

    /**
     * 判题
     * @param questionSubmitId
     * @return
     */
    QuestionSubmit doJudge(long questionSubmitId);

    /**
     * 运行示例用例
     * @param questionSubmitAddRequest
     * @return
     */
    ExecuteCodeResponse runQuestion(QuestionSubmitAddRequest questionSubmitAddRequest);
}
