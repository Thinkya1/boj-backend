package biny.biny.model.dto.ai;

import java.io.Serializable;
import lombok.Data;

/**
 * AI 对话消息
 */
@Data
public class AiChatMessage implements Serializable {

    /**
     * 角色：user / assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    private static final long serialVersionUID = 1L;
}
