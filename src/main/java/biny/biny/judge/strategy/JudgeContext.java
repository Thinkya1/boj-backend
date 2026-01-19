package biny.biny.judge.strategy;

import biny.biny.model.dto.question.JudgeCase;
import biny.biny.judge.codesandbox.model.JudgeInfo;
import biny.biny.model.entity.Question;
import biny.biny.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

}
