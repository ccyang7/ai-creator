package org.cy.aicreator.core;

import jakarta.annotation.Resource;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCreatorFacadeTest {

    @Resource
    private AiCreatorFacade aiCreatorFacade;


    @Test
    void generateAndSaveCode() {
        File file = aiCreatorFacade.generateAndSaveCode("生成一个登录页面", GenerTypeEnum.HTML);
        Assertions.assertNotNull(file);
    }


    @Test
    void generateAndSaveMultiFileCode() {
        File file = aiCreatorFacade.generateAndSaveCode("生成一个登录页面", GenerTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }


}