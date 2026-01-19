package biny.biny.model.dto.questionsubmit;

import biny.biny.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author biny
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 提交状态
     */
    private Integer status;

    /**
     * 题目 id
     */
    private Long questionId;

    /**
     * 题号
     */
    private Integer questionNumber;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}