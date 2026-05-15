package com.nev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author Lion Li (origin)
 * @author NEV-v2 (renamed)
 */

@SpringBootApplication
public class NevApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NevApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  NEV-v2 启动成功   ლ(´ڡ`ლ)ﾞ");
    }

}
