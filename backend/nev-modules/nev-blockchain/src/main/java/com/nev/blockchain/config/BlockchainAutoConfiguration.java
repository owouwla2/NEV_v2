package com.nev.blockchain.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * nev-blockchain 模块自动装配入口
 *
 * - 启用 BlockchainProperties 配置绑定
 * - 扫描 com.nev.blockchain 下的所有 @Component / @Service
 *
 * 由 ruoyi-admin 启动类（@SpringBootApplication("com.nev")）自动扫描到本类
 *
 * @author NEV-v2
 */
@Configuration
@EnableConfigurationProperties(BlockchainProperties.class)
@ComponentScan("com.nev.blockchain")
public class BlockchainAutoConfiguration {
}
