package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SpringbootRedisLettuceDemoApplicationTests {

    @Autowired
    public RedisTemplate redisTemplate;
    @Test
    void contextLoads() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
//        redisTemplate.opsForValue().get("hello");
        System.out.println(factory);

        redisTemplate.opsForValue().set("hello","测试哨兵模式");

    }

}
