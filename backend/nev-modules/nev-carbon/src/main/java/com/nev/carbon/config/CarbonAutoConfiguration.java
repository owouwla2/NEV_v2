package com.nev.carbon.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * nev-carbon 自动装配入口
 *
 * @author NEV-v2
 */
@Configuration
@ComponentScan("com.nev.carbon")
@MapperScan("com.nev.carbon.mapper")
public class CarbonAutoConfiguration {
}
