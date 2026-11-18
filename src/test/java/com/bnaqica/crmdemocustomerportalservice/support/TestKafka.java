package com.bnaqica.crmdemocustomerportalservice.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.kafka.KafkaContainer;

/** Reads what the application published, from the beginning of a topic, with a throwaway consumer group. */
public final class TestKafka {

    private TestKafka() {}

    /**
     * Reads from the beginning of {@code topic} until a record matches, or {@code timeout} passes, then a few seconds
     * more so duplicates show up too. Returns the matching records. Joining a new consumer group on a fresh broker
     * can take a while, so a fixed short read isn't enough.
     */
    public static List<ConsumerRecord<String, String>> readMatching(
            KafkaContainer kafka,
            String topic,
            java.util.function.Predicate<ConsumerRecord<String, String>> matches,
            Duration timeout) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        List<ConsumerRecord<String, String>> found = new ArrayList<>();
        try (var consumer = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            long deadline = System.nanoTime() + timeout.toNanos();
            Long settleUntil = null;
            while (System.nanoTime() < (settleUntil != null ? settleUntil : deadline)) {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (matches.test(record)) {
                        found.add(record);
                    }
                });
                if (settleUntil == null && !found.isEmpty()) {
                    settleUntil = System.nanoTime() + Duration.ofSeconds(3).toNanos();
                }
            }
        }
        return found;
    }

    /** Header value as a string, or null. */
    public static String header(ConsumerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
