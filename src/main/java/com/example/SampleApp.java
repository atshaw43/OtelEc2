package com.example;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;

public class SampleApp extends SampleAppHelper {
    public static void main(String[] args) throws Exception {
        Tracer tracer = openTelemetry.getTracer(
                "instrumentation-library-name",
                "1.0.0");

        // Create our upstream span
        SpanBuilder upstreamSpanBuilder = tracer.spanBuilder("Upstream Span");
        Span upstreamSpan = upstreamSpanBuilder.startSpan();
        upstreamSpan.end();

        // Create our downstream span
        SpanBuilder downstreamSpanBuilder = tracer.spanBuilder("Downstream Span");
        downstreamSpanBuilder.addLink(upstreamSpan.getSpanContext());
        Span downstreamSpan = downstreamSpanBuilder.startSpan();
        downstreamSpan.end();

        // Sleep so the segments have time to emit to the Collector
        Thread.sleep(5000);
    }
}
