package com.example.paymentservice.integration;

import com.example.paymentservice.PaymentServiceApplication;
import com.example.paymentservice.service.PaymentProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@ActiveProfiles("test")  // if you named the file application-test.yaml
@SpringBootTest(
        classes = PaymentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedKafka(partitions = 1, topics = { "payments" })
public class PaymentProducerIntegrationTest {

    @Autowired
    private PaymentProducer paymentProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private KafkaMessageListenerContainer<String, UUID> container;
    private ConsumerRecord<String, UUID> singleRecord;

    @BeforeEach
    void setUp() {
        // 1️⃣ Configure consumer properties manually
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testGroup");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);

        // 2️⃣ Create container
        ContainerProperties containerProps = new ContainerProperties("payments");
        DefaultKafkaConsumerFactory<String, UUID> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        // 3️⃣ Set listener (avoid lambda issue)
        container.setupMessageListener(new MessageListener<String, UUID>() {
            @Override
            public void onMessage(ConsumerRecord<String, UUID> record) {
                singleRecord = record; // capture the message
            }
        });

        container.start();

        // 4️⃣ Wait until container is assigned partitions
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        container.stop();
    }

    @Test
    void testSendPaymentEvent() throws InterruptedException {
        // Given
        UUID paymentId = UUID.randomUUID();

        // When
        paymentProducer.sendPaymentEvent(paymentId);

        // Small delay to allow the message to arrive
        Thread.sleep(2000);

        // Then
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.value()).isEqualTo(paymentId);
        assertThat(singleRecord.topic()).isEqualTo("payments");
    }
}
