package com.rohon.server.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://124.222.42.190:6379")
                .setPassword("zyf19980820");
        return Redisson.create(config);
    }


    //集群配置
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.useClusterServers()
//                .setScanInterval(2000)
//                .addNodeAddress("redis://10.211.55.4:6379", "redis://redis://10.211.55.4:6380")
//                .addNodeAddress("redis://redis://10.211.55.4:6381");
//        RedissonClient redisson = Redisson.create(config);
//        return redisson;
//    }
}
