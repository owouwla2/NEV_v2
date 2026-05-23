package com.nev.battery.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.mybatis.spring.annotation.MapperScan;

/**
 * nev-battery 自动装配入口
 *
 * @author NEV-v2
 */
@Configuration
@ComponentScan("com.nev.battery")
@MapperScan("com.nev.battery.mapper")
public class BatteryAutoConfiguration {
}
