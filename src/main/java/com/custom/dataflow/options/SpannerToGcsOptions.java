package com.custom.dataflow.options;

import com.google.cloud.spanner.Options.RpcPriority;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

public interface SpannerToGcsOptions extends PipelineOptions {

  @Description("Cloud Spanner Project Id")
  @Validation.Required
  String getSpannerProjectId();
  void setSpannerProjectId(String value);

  @Description("Cloud Spanner Instance ID")
  @Validation.Required
  String getSpannerInstanceId();
  void setSpannerInstanceId(String value);

  @Description("Cloud Spanner Database ID")
  @Validation.Required
  String getSpannerDatabaseId();
  void setSpannerDatabaseId(String value);

  @Description("Cloud Spanner SQL Query")
  @Validation.Required
  String getSqlQuery();
  void setSqlQuery(String value);

  @Description("Cloud Storage Output Directory")
  @Validation.Required
  String getGcsOutput();
  void setGcsOutput(String value);

  @Description("Priority for Spanner RPC invocations")
  @Default.Enum("HIGH")
  RpcPriority getSpannerPriority();
  void setSpannerPriority(RpcPriority value);
}