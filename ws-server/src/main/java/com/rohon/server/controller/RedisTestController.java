package com.rohon.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequestMapping("/redis")
@RestController
public class RedisTestController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    private DefaultRedisScript<Long> script;


    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1]\n" +
                "    then\n" +
                "        return redis.call('del', KEYS[1])\n" +
                "    else\n" +
                "        return 0\n" +
                "end");
    }


    /**
     *  setNX + Lua 实现分布式锁(redis单机模式可用)
     * @link https://www.cnblogs.com/niceyoo/p/13711149.html
     */
    @PostMapping("/test1")
    public String test1(){
        String key = "key";
        String value = UUID.randomUUID().toString().replace("-", "");
        /*
         * setIfAbsent <=> SET key value [NX] [XX] [EX <seconds>] [PX [milliseconds]]
         * set expire time 5 min
         */
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value, 2000, TimeUnit.SECONDS);
        if (success) {
            log.info("{} 锁定成功，开始处理", key);
            try {
                // 处理业务逻辑。。
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 释放锁
            String res = (String) redisTemplate.opsForValue().get(key);
            if (value.equals(res)) {
                System.out.println("=======res: "+res);
                ArrayList<String> keys = new ArrayList<>();
                keys.add(key);
                // 调用lua脚本释放
                Long execute = (Long) redisTemplate.execute(script, keys, res);
                System.out.println("execute执行结果，1表示执行del，0表示未执行 ===== " + execute);
                log.info("{} 解锁成功，结束处理业务", key);
            }
            return "SUCCESS";
        } else {
            log.info("{} 获取锁失败", key);
            return "请稍后再试...";
        }
    }

    /**
     * Redission实现分布式锁
     * @link https://www.cnblogs.com/niceyoo/p/13736140.html
     */
    @PostMapping("/test2")
    public String test2() {
        String uuid = UUID.randomUUID().toString();
        log.info(uuid);
        RLock lock = redissonClient.getLock("id: " + uuid);
        try {
            lock.lock();
            log.info("locked！！");
            Thread.sleep(20000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return "SUCCESS";
    }
}
