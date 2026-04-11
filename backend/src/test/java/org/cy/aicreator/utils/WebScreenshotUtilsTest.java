package org.cy.aicreator.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebScreenshotUtilsTest {


        @Test
        void saveWebPageScreenshot() {
            String testUrl = "https://www.codefather.cn";
            String webPageScreenshot = WebScreenshotUtils.captureAndCompressWebPage(testUrl);
            Assertions.assertNotNull(webPageScreenshot);
        }
    }
