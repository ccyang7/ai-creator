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
    void generateAndSaveHtmlCode() {
        File file = aiCreatorFacade.generateAndSaveCode("生成一个登录页面", GenerTypeEnum.HTML);
        Assertions.assertNotNull(file);
    }


    @Test
    void generateAndSaveMultiFileCode() {
        File file = aiCreatorFacade.generateAndSaveCode("生成一个登录页面", GenerTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }


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
}