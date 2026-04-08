package org.cy.aicreator.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.cy.aicreator.model.dto.chatHistory.ChatHistoryQueryDTO;
import org.cy.aicreator.model.entity.ChatHistory;
import org.cy.aicreator.model.entity.User;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author cy
 * @since 2026-03-30
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话消息。
     *
     * @param appId       应用ID
     * @param userId      用户ID
     * @param message     消息内容
     * @param messageType 消息类型
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, Long userId, String message, String messageType);


    /**
     * 根据应用ID删除对话消息。
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);


    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryDTO 对话历史查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryDTO chatHistoryQueryDTO);

    /**
     * 获取应用对话历史分页列表
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser      登录用户
     * @return 对话历史分页列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    /**
     * 加载对话历史到记忆中
     *
     * @param appId           应用ID
     * @param chatMemory      记忆对象
     * @param maxMessages     最大消息数
     * @return 加载的消息数
     */
    int loadChatHistoryMemory(Long appId, MessageWindowChatMemory chatMemory, int maxMessages);
}
