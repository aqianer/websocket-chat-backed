@echo off
java -cp "target/classes;target/dependency/*" com.example.websocketchatbacked.flink.util.KafkaTopicConsumer %*
pause
