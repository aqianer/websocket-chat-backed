@echo off
java -cp "target/flink-billing-complete.jar;target/lib/*" com.example.websocketchatbacked.flink.job.BillingAggregationJob %*
pause
