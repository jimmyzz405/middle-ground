package com.rohon.server.controller;

import io.swagger.annotations.Api;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(rollbackFor = Exception.class)
    public void sendMsg3(@RequestParam String msg) {
        // 在事务中执行发送消息
        //// 事务开启需要配置：1. producer.transaction-id-prefix 2.producer.ack设为all 3.producer.retries设置大于0
        kafkaTemplate.executeInTransaction(operations -> operations.send("topic-test-3", msg));
        throw new RuntimeException("kafka事务test！");

//        kafkaTemplate.executeInTransaction(o -> {
//            o.send("topic-test-3", msg);
//            throw new RuntimeException("异常");
//        });
    }

    /**
     * 监听指定topic并消费数据，打印到log中
     *
     * * 批量消费时，必须使用 List 接收，否则会抛异常。
     * * 即如果配置文件配置的是批量消费(spring.kafka.listener.type=batch)，则监听时必须使用 list 接收
     */
    @KafkaListener(topics = {"topic-test-1", "topic-test-2", "topic-test-3"})
    public void onMessageReceived(ConsumerRecord<?, ?> record) {
        // 消费的哪个topic、partition的消息,打印出消息内容
        log.info("消费信息！topic: {}, partition: {}, key: {}, value: {}", record.topic(), record.partition(), record.key(), record.value());
    }


//    /**
//     * 指定topic、partition、offset消费
//     *
//     * 同时监听topic1和topic2，监听topic1的0号分区、topic2的 "0号和1号" 分区，指向1号分区的offset初始值为8
//     **/
//    @KafkaListener(id = "consumer1",groupId = "felix-group",topicPartitions = {
//            @TopicPartition(topic = "topic1", partitions = { "0" }),
//            @TopicPartition(topic = "topic2", partitions = "0", partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "8"))
//    })
//    public void onMessage2(ConsumerRecord<?, ?> record) {
//        System.out.println("topic:" + record.topic() + "|partition:" + record.partition() + "|offset:" + record.offset() + "|value:" + record.value());
//    }
}
