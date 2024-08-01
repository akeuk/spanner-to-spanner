package com.custom.dataflow.templates;

import com.custom.dataflow.options.SpannerToGcsOptions;
import com.custom.dataflow.utils.SpannerUtils;
import org.apache.beam.sdk.io.gcp.spanner.SpannerConfig;
import org.apache.beam.sdk.io.gcp.spanner.SpannerIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.TypeDescriptors;

public class SpannerToGcsCsv {
  public static void main(String[] args) {

    //Pipeline Options
    PipelineOptionsFactory.register(SpannerToGcsOptions.class);
    SpannerToGcsOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(SpannerToGcsOptions.class);
    
    //Additional Configurations
    SpannerConfig spannerConfig =
        SpannerConfig.create()
            .withProjectId(options.getSpannerProjectId())
            .withDatabaseId(options.getSpannerDatabaseId())
            .withInstanceId(options.getSpannerInstanceId())
            .withRpcPriority(options.getSpannerPriority());

    //Pipeline Start
    Pipeline pipeline = Pipeline.create(options);
    
    pipeline
        .apply("Read Cloud Spanner", 
          SpannerIO.read()
            .withSpannerConfig(spannerConfig)
            .withBatching(false) // without Parallel Read API
            .withQuery(options.getSqlQuery()) // specific query
            )
        .apply("Struct To Csv",
          MapElements.into(TypeDescriptors.strings())
              .via(struct -> (SpannerUtils.convertStructToCsv(struct)))
          )
        .apply(
          "Write to Cloud Storage",
          TextIO.write()
              .to(options.getGcsOutput())
              .withSuffix(".csv")
              .withoutSharding()); //Write only a single CSV file

    pipeline.run();
  }
}