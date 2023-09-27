package com.example;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class Lambda {
    static final String lambdaQueue = "https://sqs.us-west-1.amazonaws.com/103216442713/QueueForLambda";
    //static final String lambdaQueue = "https://sqs.us-west-1.amazonaws.com/201075033921/AdamTest";

    public static void main(String[] args) throws Exception {
        //singleTest();
        //singleTestUnsampled();
        batchSampled();
        //mixOfSampledAndNotSampled();

        Thread.sleep(5000);
    }

    public static void singleTest() {
        sendMessage("Test1");
    }

    public static void singleTestUnsampled() {
        Span span = SampleAppHelper.startSpanUnsampled("Unsampled Span");

        try (Scope scope = span.makeCurrent()) {
            sendMessage("Unsampled");
        }

        span.end();
    }

    public static void batchSampled() {
        Span span = SampleAppHelper.startSpanSampled("Upstream");

        try (Scope scope = span.makeCurrent()) {
            SendMessageBatchRequest sendMessageRequest = SendMessageBatchRequest.builder()
                    .queueUrl(lambdaQueue)
                    .entries(
                            SendMessageBatchRequestEntry.builder().id("A").messageBody("Test1").build(),
                            SendMessageBatchRequestEntry.builder().id("B").messageBody("Test2").build(),
                            SendMessageBatchRequestEntry.builder().id("C").messageBody("Test3").build(),
                            SendMessageBatchRequestEntry.builder().id("D").messageBody("Test4").build(),
                            SendMessageBatchRequestEntry.builder().id("E").messageBody("Test5").build()).build();

            SampleAppHelper.sqsAutoinstrumentation.sendMessageBatch(sendMessageRequest);
        }
        span.end();
    }

    public static void mixOfSampledAndNotSampled() {
        Span spanUnsampled = SampleAppHelper.startSpanUnsampled("Unsampled Span");
        try (Scope scope = spanUnsampled.makeCurrent()) {
            sendMessage("Unsampled1");
            sendMessage("Unsampled2");
            sendMessage("Unsampled3");
        }
        spanUnsampled.end();

        Span spanSampled = SampleAppHelper.startSpanSampled("Sampled Span");
        try (Scope scope = spanSampled.makeCurrent()) {
            sendMessage("Sampled1");
            sendMessage("Sampled2");
            sendMessage("Sampled3");
        }
        spanSampled.end();
    }

    public static void sendMessage(String body) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(lambdaQueue)
                .messageBody(body)
                .build();

        System.out.println("Sending: " + sendMessageRequest.messageBody());

        SampleAppHelper.sqsAutoinstrumentation.sendMessage(sendMessageRequest);
    }
}
