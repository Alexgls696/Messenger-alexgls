package com.alexgls.springboot.loggingbatchconsumerservice.service;

import com.alexgls.springboot.loggingbatchconsumerservice.dto.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogBatchWriter {

    private final JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "logging-batch-consumer-topic", containerFactory = "concurrentKafkaListenerContainerFactory", groupId = "logging-batch-consumer-group")
    public void listen(List<ConsumerRecord<String, LogRecord>>records, Acknowledgment ack) {
        log.info("Received batch of {} logs", records.size());

        String sql = "INSERT INTO logs (ts, level, service_name, message, trace_id, context) " +
                "VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))";

        try{
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    LogRecord log = records.get(i).value();
                    ps.setObject(1, log.timestamp());
                    ps.setString(2, log.level());
                    ps.setString(3, log.serviceName());
                    ps.setString(4, log.message());
                    ps.setString(5, log.traceId());
                    ps.setString(6, log.context());
                }

                @Override
                public int getBatchSize() {
                    return records.size();
                }
            });
            ack.acknowledge();
        }catch (Exception exception){
            log.error("Error writing batch to DB", exception);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // В час ночи каждый день
    public void runPartmanMaintenance() {
        log.info("Starting pg_partman maintenance...");
        jdbcTemplate.execute("SELECT partman.run_maintenance()");
        log.info("pg_partman maintenance finished.");
    }

}
