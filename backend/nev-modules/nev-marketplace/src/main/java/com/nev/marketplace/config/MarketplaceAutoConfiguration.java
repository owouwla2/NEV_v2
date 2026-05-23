package com.nev.marketplace.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * nev-marketplace 自动装配入口
 *
 * @author NEV-v2
 */
@Configuration
@ComponentScan("com.nev.marketplace")
@MapperScan("com.nev.marketplace.mapper")
public class MarketplaceAutoConfiguration {
}
