package com.seckilling.mq;

import com.alibaba.fastjson.JSON;
import com.seckilling.error.BusinessException;
import com.seckilling.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
public class MQProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Resource
    private OrderService orderService;

    @PostConstruct
    public void init() throws MQClientException {
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object args) {
                //create order here
                Map argsMap = (HashMap) args;
                Integer userId = (Integer) argsMap.get("userId");
                Integer itemId = (Integer) argsMap.get("itemId");
                Integer quantity = (Integer) argsMap.get("quantity");
                Integer promoId = (Integer) argsMap.get("promoId");

                try {
                    orderService.createOder(userId, itemId, quantity, promoId);
                } catch (BusinessException e) {
                    e.printStackTrace();
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                return null;
            }
        });
        transactionMQProducer.start();
    }


    //async stock in transactional manner
    public boolean transactionAsyncDeductStock(Integer userId, Integer itemId, Integer quantity, Integer promoId) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("quantity", quantity);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("userId", userId);
        argsMap.put("itemId", itemId);
        argsMap.put("quantity", quantity);
        argsMap.put("promoId", promoId);

        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        TransactionSendResult sendResult;
        try {
            //2 phrases, first in prepare phrase, execute executeLocalTransaction()
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        return sendResult != null && sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE;
    }


    //async stock
    public boolean asyncDeductStock(Integer itemId, Integer quantity) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("quantity", quantity);

        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));

        try {
            producer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
