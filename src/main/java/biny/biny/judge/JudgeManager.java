package biny.biny.judge;

import biny.biny.judge.strategy.DefaultJudgeStrategy;
import biny.biny.judge.strategy.JavaLanguageJudgeStrategy;
import biny.biny.judge.strategy.JudgeContext;
import biny.biny.judge.strategy.JudgeStrategy;
import biny.biny.judge.codesandbox.model.JudgeInfo;
import biny.biny.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * 判题管理（简化调用）
 */
@Service
public class JudgeManager {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if ("java".equals(language)) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }

}
