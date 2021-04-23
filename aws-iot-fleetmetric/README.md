# AWS::IoT::FleetMetric

1. The JSON schema of fleet metric resource is located in `aws-iot-fleetmetric.json`
1. Check the src folder for resource handlers implementations
1. For local testing, check CFN CLI. Sample command to submit the CFN resource to your testing account `cfn submit --no-role --set-default --region us-east-1`

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.
