package org.cy.aicreator.core;

import jakarta.annotation.Resource;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCreatorFacadeTest {

    @Resource
    private AiCreatorFacade aiCreatorFacade;




    @Test
    void testGenerateAndSaveHtmlCode() {
        Flux<String> file = aiCreatorFacade.generateAndSaveCodeStream("生成一个简易登录页面", GenerTypeEnum.HTML, 1L);
        List<String> block = file.collectList().block();
        Assertions.assertNotNull(block);
        String completeFile = String.join("", block);
        Assertions.assertNotNull(file);
    }

    @Test
    void testGenerateAndSaveMultiFileCode() {
        Flux<String> file = aiCreatorFacade.generateAndSaveCodeStream("生成一个简易登录页面", GenerTypeEnum.MULTI_FILE, 1L);
        List<String> block = file.collectList().block();
        Assertions.assertNotNull(block);
        String completeFile = String.join("", block);
        Assertions.assertNotNull(file);
    }


    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCreatorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                GenerTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}