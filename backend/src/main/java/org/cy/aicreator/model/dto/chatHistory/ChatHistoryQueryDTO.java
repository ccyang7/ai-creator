package org.cy.aicreator.model.dto.chatHistory;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cy.aicreator.common.PageRequest;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 对话历史游标查询请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryDTO extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 游标查询 - 最后一条记录的创建时间
     * 用于分页查询，获取早于此时间的记录
     */
    private LocalDateTime lastCreateTime;


}
