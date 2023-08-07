# OtelEc2
1. Setup your AWS credentials and region. https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html
2. Pull the collector down locally. https://github.com/open-telemetry/opentelemetry-collector-contrib
3. Setup the region for your collector. opentelemetry-collector/examples/local/otel-config.yaml
4. Run the collector "make run".
5. Change the region of the SampleApp in SampleAppHelper.
6. Run the SampleApp. Right click SampleApp.java -> run.