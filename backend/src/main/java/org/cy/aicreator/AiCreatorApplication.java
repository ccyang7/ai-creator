package org.cy.aicreator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.cy.aicreator.mapper")
public class AiCreatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCreatorApplication.class, args);
    }

}
