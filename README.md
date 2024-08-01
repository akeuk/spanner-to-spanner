# Dataflow Flex Template: Cloud Spanner to Cloud Storage with a CSV file
Objective: Dataflow Template: Cloud Spanner to Cloud Storage with a CSV file

Reference: [Dataflow: Build and run a Flex Template](https://cloud.google.com/dataflow/docs/guides/templates/using-flex-templates)

## Prepare environment
* JDK version 11
* Maven
* Google Cloud IAM Service Account with permissions
    * Cloud Spanner
    * Cloud Storage
    * Dataflow
    * Cloud Build
    * Artifact Registry
* Cloud Storage with bucket
* Cloud Spanner with tables
* Artifact Registry repo

## Direct Runner
* Prepare `gcloud` configuration before run
    * $ gcloud config set project <GOOGLE_CLOUD_PROJECT>
    * $ gcloud auth application-default login
    * $ gcloud auth list
```
$ mvn compile exec:java \
-Dexec.mainClass=com.custom.dataflow.templates.SpannerToGcsCsv \
-Dexec.args="--runner=DirectRunner \
--region=<GOOGLE_CLOUD_REGION> \
--project=<GOOGLE_CLOUD_PROJECT> \
--stagingLocation=gs://<GCS_BUCKET>/staging \
--tempLocation=gs://<GCS_BUCKET>/temp \
--spannerProjectId=<CLOUD_SPANNER_PROJECT_ID> \
--spannerInstanceId=<CLOUD_SPANNER_INSTANCE_ID> \
--spannerDatabaseId=<CLOUD_SPANNER_DATABASE_ID> \
--sqlQuery='<CLOUD_SPANNER_SQL_STATEMENT>' \
--gcsOutput=gs://<GCS_BUCKET>/output/output"
```

## Compile and build a package
```
$ mvn clean package
```

## Build a template
```
gcloud dataflow flex-template build gs://<GCS_BUCKET>/templates/spanner-to-gcs-csv.json \
 --image-gcr-path "<GOOGLE_CLOUD_REGION>-docker.pkg.dev/<GOOGLE_CLOUD_PROJECT>/<ARTIFACT_REPO>/spanner-to-gcs-csv:latest" \
 --sdk-language "JAVA" \
 --flex-template-base-image JAVA11 \
 --metadata-file "metadata.json" \
 --jar "target/spanner-to-gcs-csv-1.0.jar" \
 --env FLEX_TEMPLATE_JAVA_MAIN_CLASS=com.custom.dataflow.templates.SpannerToGcsCsv
```

## Run a custom template. 
Note: You can also run it via **Google Cloud Console** with "Custom Template" in Template Path, then choose location of the `spanner-to-gcs-csv.json` file.

For `gcloud` command, see below.
```
gcloud dataflow flex-template run "nutcha-custom-df-`date +%Y%m%d-%H%M%S`"  \
    --template-file-gcs-location "gs://<GCS_BUCKET>/templates/spanner-to-gcs-csv.json" \
	--region <GOOGLE_CLOUD_REGION> \
	--service-account-email="<GOOGLE_CLOUD_SERVICE_ACCOUNT_EMAIL>" \
    --parameters spannerProjectId="<CLOUD_SPANNER_PROJECT_ID>" \
	--parameters spannerInstanceId="<CLOUD_SPANNER_INSTANCE_ID>" \
	--parameters spannerDatabaseId="<CLOUD_SPANNER_DATABASE_ID>" \
	--parameters sqlQuery="<CLOUD_SPANNER_SQL_STATEMENT>" \
	--parameters gcsOutput="gs://<GCS_BUCKET>/output/output"
```