package org.cy.aicreator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.constant.UserConstant;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.exception.ThrowUtils;
import org.cy.aicreator.model.dto.chatHistory.ChatHistoryQueryDTO;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.ChatHistory;
import org.cy.aicreator.mapper.ChatHistoryMapper;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.enums.ChatHistoryMessageTypeEnum;
import org.cy.aicreator.service.AppService;
import org.cy.aicreator.service.ChatHistoryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author cy
 * @since 2026-03-30
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAM_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryDTO queryRequest = new ChatHistoryQueryDTO();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 加载对话历史到记忆中
     *
     * @param appId           应用ID
     * @param chatMemory      记忆对象
     * @param maxMessages     最大消息数
     * @return 加载的消息数
     */
    @Override
    public int loadChatHistoryMemory(Long appId, MessageWindowChatMemory chatMemory, int maxMessages) {
        try {
            // 直接构造查询条件，起始点为1，而不是0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1,maxMessages);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)){
                return 0;
            }
            // 反转列表，确保时间正序
            historyList= CollUtil.reverse(historyList);
            // 按时间顺序添加到记忆中
            int count = 0;
            // 清空记忆
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if(ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())){
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    count++;
                }else if(ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())){
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    count++;
                }
            }
            log.info("加载了appId:{},{}条对话历史记录",appId,count);
            return count;
        } catch (Exception e) {
            log.error("加载appId:{}的对话历史出错，原因：{}",appId,e.getMessage());
            return 0;
        }
    }


    /**
     * 添加对话消息
     *
     * @param appId       应用ID
     * @param userId      用户ID
     * @param message     消息内容
     * @param messageType 消息类型(user/ai)
     * @return 是否添加成功
     */
    @Override
    public boolean addChatMessage(Long appId, Long userId, String message, String messageType) {
        // 判断参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAM_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAM_ERROR, "消息类型不能为空");

        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum enumByValue = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAM_ERROR, messageType + "消息类型无效");

        // 添加消息
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .build();

        return this.save(chatHistory);
    }

    /**
     * 根据应用ID删除对话消息
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID无效");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("app_id", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryDTO 对话消息查询条件
     * @return 查询包装类
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryDTO chatHistoryQueryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryDTO == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryDTO.getId();
        String message = chatHistoryQueryDTO.getMessage();
        String messageType = chatHistoryQueryDTO.getMessageType();
        Long appId = chatHistoryQueryDTO.getAppId();
        Long userId = chatHistoryQueryDTO.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryDTO.getLastCreateTime();
        String sortField = chatHistoryQueryDTO.getSortField();
        String sortOrder = chatHistoryQueryDTO.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("message_type", messageType)
                .eq("app_id", appId)
                .eq("user_id", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("create_time", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("create_time", false);
        }
        return queryWrapper;
    }


}
