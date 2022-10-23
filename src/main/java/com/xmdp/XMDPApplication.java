package com.xmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.xmdp.mapper")
@SpringBootApplication
public class XMDPApplication {

    public static void main(String[] args) {
        SpringApplication.run(XMDPApplication.class, args);
    }

}
