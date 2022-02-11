package com.rohon.server.controller;

import io.swagger.annotations.Api;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaSendCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * KAFKA 练习
 * @link https://blog.csdn.net/yuanlong122716/article/details/105160545/
 */
@RestController
@Slf4j
@Api("kafka")
@RequestMapping("/kafka")
public class KafkaTestController {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 生产者-简单发送消息
     */
    @PostMapping("/send/test-1")
    public void sendMsg1(@RequestParam String msg) {
        kafkaTemplate.send("topic-test-1", msg);
    }

    @PostMapping("/send/test-2")
    public void sendMsg2(@RequestParam String callbackMsg) {
        kafkaTemplate.send("topic-test-2", callbackMsg).addCallback(success -> {
            long offset = success.getRecordMetadata().offset();
            int partition = success.getRecordMetadata().partition();
            String topic = success.getRecordMetadata().topic();
            log.info("发送成功. topic:{}, offset:{}, partition:{}", topic, offset, partition);
        }, failure -> {
            log.error("发送失败！");
            failure.printStackTrace();
        });
    }

    @PostMapping("/send/test-3")
    public void sendMsg3(@RequestParam String msg) {
        // 在事务中执行发送消息
        //// TODO: 2022/2/11 事务开启有问题
        log.info(""+kafkaTemplate.isTransactional());
        log.info(""+kafkaTemplate.inTransaction());
        kafkaTemplate.executeInTransaction(operations -> operations.send("topic-test-3", msg));
        throw new RuntimeException("kafka事务test！");
    }

    /**
     * 监听指定topic并消费数据，打印到log中
     */
    @KafkaListener(topics = {"topic-test-1", "topic-test-2", "topic-test-3"})
    public void onMessageReceived(ConsumerRecord<?, ?> record) {
        // 消费的哪个topic、partition的消息,打印出消息内容
        log.info("消费信息！topic: {}, partition: {}, key: {}, value: {}", record.topic(), record.partition(), record.key(), record.value());
    }
}
