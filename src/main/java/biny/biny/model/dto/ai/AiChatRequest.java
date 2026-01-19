package biny.biny.model.dto.ai;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * AI 对话请求
 */
@Data
public class AiChatRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 当前代码
     */
    private String code;

    /**
     * 用户问题
     */
    private String prompt;

    /**
     * 历史消息
     */
    private List<AiChatMessage> history;

    private static final long serialVersionUID = 1L;
}
