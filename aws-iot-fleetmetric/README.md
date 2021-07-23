# AWS::IoT::FleetMetric

1. Ensure you have the latest CFN CLI https://github.com/aws-cloudformation/cloudformation-cli. Sample command for upgrade `pip install --upgrade cloudformation-cli cloudformation-cli-java-plugin`
1. The JSON schema of fleet metric resource is in `aws-iot-fleetmetric.json`
1. Check the src folder for resource handlers implementations
1. For local testing, check CFN CLI.
   1. Public doc reference https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough
   1. Inside folder `aws-iot-fleetmetric`
   1. In a new terminal session, run the following command: `sam local start-lambda`
   1. Build package `mvn clean && mvn package`
   1. General resource schema `cfn generate`
   1. Run contract tests `cfn test`
   1. Submit CFN resource to your testing account `cfn submit --no-role --set-default --region us-east-1`

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.
