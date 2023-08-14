# OtelEc2
1. Setup your AWS credentials and region. https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html
2. Pull the collector contrib down locally. https://github.com/open-telemetry/opentelemetry-collector-contrib
3. Pull the collector down locally. https://github.com/open-telemetry/opentelemetry-collector
4. Setup the region for your collector in the config. opentelemetry-collector/examples/local/otel-config.yaml
5. Point your collector contrib makefile to the config in the collector.
Open the Makefile in collector contrib and replace the config option under run to the one in your collector project.
```
.PHONY: run
run:
	cd ./cmd/otelcontribcol && GO111MODULE=on $(GOCMD) run --race . --config /path/to/workspace/opentelemetry-collector/examples/local/otel-config.yaml ${RUN_ARGS}
```
6. Run the collector with "make run" in the top directory of the collector contrib.
7. Change the region of the SampleApp in SampleAppHelper.
8. Run the SampleApp. Right click SampleApp.java -> run.
