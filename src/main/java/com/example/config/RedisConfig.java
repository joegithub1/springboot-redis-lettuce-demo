package com.example.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName RedisConfig
 * @Description:
 * @Author huangjian
 * @Date 2021/7/1
 **/
@Configuration
public class RedisConfig {

    @Autowired
    private Environment environment;

    @Value("${spring.redis.model}")
    private String model;

    @Value("${spring.redis.timeout}")
    private long timeout;
    @Value("${spring.redis.lettuce.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle}")
    private int minIdle;

    @Value("${spring.redis.lettuce.pool.max-active}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-wait}")
    private long maxWait;

    @Bean
    public GenericObjectPoolConfig genericObjectPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxTotal(maxActive);
        config.setMaxWaitMillis(maxWait);
        //???borrow??????jedis??????????????????????????????validate??????????????????true ????????????jedis????????????????????????
        config.setTestOnBorrow(true);
        //???return???pool????????????????????????validate?????????
        config.setTestOnReturn(true);
        //???????????????????????????
        config.setTestWhileIdle(true);
        return config;
    }

    @Bean
    LettuceConnectionFactory lettuceConnectionFactory(GenericObjectPoolConfig genericObjectPoolConfig) {
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .poolConfig(genericObjectPoolConfig)
                .build();

        LettuceConnectionFactory factory = null;
        if("single".equals(model)){
            //????????????
            Integer database = Integer.valueOf(environment.getProperty("spring.redis.database",""));
            String host = environment.getProperty("spring.redis.host","");
            int port = Integer.valueOf(environment.getProperty("spring.redis.port",""));
            String password = environment.getProperty("spring.redis.password","");

            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            redisStandaloneConfiguration.setDatabase(database);
            redisStandaloneConfiguration.setHostName(host);
            redisStandaloneConfiguration.setPort(port);
            if("" != password && null != password){
                redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
            }
            factory = new LettuceConnectionFactory(redisStandaloneConfiguration,clientConfig);
        }else if("sentinel".equals(model)){
            String masterName = environment.getProperty("spring.redis.sentinel.master", "");
            String sentinelNodes = environment.getProperty("spring.redis.sentinel.nodes","");
            String sentinePassWord = environment.getProperty("spring.redis.sentinel.password","");
            //????????????
            RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration();
            sentinelConfiguration.setMaster(masterName);
            if(null != sentinePassWord && !"".equals(sentinePassWord)){
                sentinelConfiguration.setPassword(sentinePassWord);
            }
            //????????????
            String[] serverArray = sentinelNodes.split(",");
            Set<RedisNode> nodes = new HashSet<RedisNode>();
            for (String ipPort : serverArray) {
                String[] ipAndPort = ipPort.split(":");
                nodes.add(new RedisNode(ipAndPort[0].trim(), Integer.valueOf(ipAndPort[1])));
            }
            sentinelConfiguration.setSentinels(nodes);
            factory = new LettuceConnectionFactory(sentinelConfiguration,clientConfig);
        }
        return factory;
    }
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        // ??????value????????????????????? key??????????????????
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
