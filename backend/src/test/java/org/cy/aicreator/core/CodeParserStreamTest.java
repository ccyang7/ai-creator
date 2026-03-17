package org.cy.aicreator.core;

import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CodeParserStreamTest {

    @Test
    void testParseHtmlCode() {

        String content = """
                这是AI生成的网页
                
                ```html
                <html>
                <body>
                <h1>Hello AI</h1>
                </body>
                </html>
                ```
                """;

        HtmlCodeResult result = CodeParserStream.parseHtmlCode(content);

        String expectedHtml = """
                <html>
                <body>
                <h1>Hello AI</h1>
                </body>
                </html>
                """.trim();

        assertNotNull(result);
        assertEquals(expectedHtml, result.getHtmlCode());
    }

    @Test
    void testParseMultiFileCode() {

        String content = """
                ```html
                <html>
                <body>Hello</body>
                </html>
                ```

                ```css
                body{
                    background:red;
                }
                ```

                ```js
                console.log("hello world");
                ```
                """;

        MultiFileCodeResult result = CodeParserStream.parseMultiFileCode(content);

        String expectedHtml = """
                <html>
                <body>Hello</body>
                </html>
                """.trim();

        String expectedCss = """
                body{
                    background:red;
                }
                """.trim();

        String expectedJs = """
                console.log("hello world");
                """.trim();

        assertNotNull(result);

        assertEquals(expectedHtml, result.getHtmlCode());
        assertEquals(expectedCss, result.getCssCode());
        assertEquals(expectedJs, result.getJsCode());
    }
}