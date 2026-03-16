package org.cy.aicreator.ai;

import jakarta.annotation.Resource;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult htmlCode = aiCodeGeneratorService.generateHtmlCode("帮我生成一个个人博客，代码量不超过300行，如果超过了，顺带输出超过的行数");
        Assertions.assertNotNull(htmlCode);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult htmlCode = aiCodeGeneratorService.generateMultiFileCode("帮我生成一个留言板，代码量不超过300行，如果超过了，顺带输出超过的行数");
        Assertions.assertNotNull(htmlCode);
    }
}