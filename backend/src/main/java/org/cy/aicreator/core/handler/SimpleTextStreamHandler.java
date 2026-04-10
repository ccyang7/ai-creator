package org.cy.aicreator.core.handler;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.enums.ChatHistoryMessageTypeEnum;
import org.cy.aicreator.service.ChatHistoryService;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();

        return originFlux.map(content -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(content);
                    return content;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, loginUser.getId(), aiResponse, ChatHistoryMessageTypeEnum.AI.getValue());
                    }
                }).doOnError(e -> {
                    //  如果AI回复失败，则添加错误消息到对话历史
                    String errorMessage = "AI 响应失败，请稍后再试" + e.getMessage();
                    chatHistoryService.addChatMessage(appId, loginUser.getId(), errorMessage, ChatHistoryMessageTypeEnum.AI.getValue());
                });
    }


}
