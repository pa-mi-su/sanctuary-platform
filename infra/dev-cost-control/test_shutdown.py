import os
import sys
import types
import unittest
from pathlib import Path
from unittest.mock import Mock, patch


TEMPLATE = Path(__file__).with_name("template.yml")


def extract_inline_lambda():
    lines = TEMPLATE.read_text(encoding="utf-8").splitlines()
    marker = "        ZipFile: |"
    start = lines.index(marker) + 1
    extracted = []
    for line in lines[start:]:
        if line == "":
            extracted.append("")
            continue
        if line.startswith("          "):
            extracted.append(line[10:])
            continue
        break
    return "\n".join(extracted) + "\n"


class FakeBoto3(types.ModuleType):
    def client(self, service_name, region_name=None):
        raise AssertionError(f"Unexpected client request: {service_name}/{region_name}")


def load_handler_module():
    environment = {
        "EXPECTED_ACCOUNT_ID": "160885294528",
        "EXPECTED_REGION": "us-east-1",
        "AWS_REGION": "us-east-1",
        "ECS_CLUSTER": "sanctuary-dev",
        "ECS_SERVICE": "sanctuary-api-dev",
        "RDS_INSTANCE": "sanctuary-dev-db",
        "POLL_SECONDS": "1",
        "MAX_WAIT_SECONDS": "5",
    }
    module = types.ModuleType("shutdown_handler")
    fake_boto3 = FakeBoto3("boto3")
    with patch.dict(os.environ, environment, clear=False), patch.dict(
        sys.modules, {"boto3": fake_boto3}
    ):
        exec(compile(extract_inline_lambda(), str(TEMPLATE), "exec"), module.__dict__)
    return module


class ShutdownFunctionTests(unittest.TestCase):
    def setUp(self):
        self.module = load_handler_module()
        self.sts = Mock()
        self.ecs = Mock()
        self.rds = Mock()
        self.sts.get_caller_identity.return_value = {"Account": "160885294528"}
        self.ecs.describe_services.return_value = {
            "failures": [],
            "services": [
                {
                    "clusterArn": "arn:aws:ecs:us-east-1:160885294528:cluster/sanctuary-dev",
                    "serviceArn": "arn:aws:ecs:us-east-1:160885294528:service/sanctuary-dev/sanctuary-api-dev",
                    "status": "ACTIVE",
                    "desiredCount": 1,
                    "runningCount": 1,
                    "pendingCount": 0,
                }
            ],
        }
        self.module._clients = Mock(return_value=(self.sts, self.ecs, self.rds))

    def database(self, state, arn="arn:aws:rds:us-east-1:160885294528:db:sanctuary-dev-db"):
        return {"DBInstances": [{"DBInstanceArn": arn, "DBInstanceStatus": state}]}

    def test_running_resources_are_stopped(self):
        self.rds.describe_db_instances.return_value = self.database("available")

        result = self.module.handler(
            {"action": "stop", "source": "test"},
            None,
        )

        self.ecs.update_service.assert_called_once_with(
            cluster="sanctuary-dev",
            service="sanctuary-api-dev",
            desiredCount=0,
        )
        self.rds.stop_db_instance.assert_called_once_with(
            DBInstanceIdentifier="sanctuary-dev-db"
        )
        self.assertEqual(result["rds"]["requested"], "stop")

    def test_already_stopped_resources_are_idempotent(self):
        self.ecs.describe_services.return_value["services"][0]["desiredCount"] = 0
        self.ecs.describe_services.return_value["services"][0]["runningCount"] = 0
        self.rds.describe_db_instances.return_value = self.database("stopped")

        result = self.module.handler({"action": "stop"}, None)

        self.ecs.update_service.assert_called_once()
        self.rds.stop_db_instance.assert_not_called()
        self.assertEqual(result["rds"]["requested"], "none")

    def test_stopping_database_is_successful_no_op(self):
        self.rds.describe_db_instances.return_value = self.database("stopping")

        result = self.module.handler({"action": "stop"}, None)

        self.rds.stop_db_instance.assert_not_called()
        self.assertEqual(result["rds"]["before"], "stopping")

    def test_transitional_database_is_polled_then_stopped(self):
        self.rds.describe_db_instances.side_effect = [
            self.database("starting"),
            self.database("available"),
        ]

        with patch.object(self.module.time, "sleep"):
            result = self.module.handler({"action": "stop"}, None)

        self.assertEqual(self.rds.describe_db_instances.call_count, 2)
        self.rds.stop_db_instance.assert_called_once()
        self.assertEqual(result["rds"]["before"], "available")

    def test_wrong_account_is_rejected_before_mutation(self):
        self.sts.get_caller_identity.return_value = {"Account": "000000000000"}

        with self.assertRaisesRegex(RuntimeError, "Refusing AWS account"):
            self.module.handler({"action": "stop"}, None)

        self.ecs.update_service.assert_not_called()
        self.rds.stop_db_instance.assert_not_called()

    def test_wrong_region_is_rejected_before_mutation(self):
        self.module.ACTUAL_REGION = "us-west-2"

        with self.assertRaisesRegex(RuntimeError, "Refusing AWS region"):
            self.module.handler({"action": "stop"}, None)

        self.ecs.update_service.assert_not_called()
        self.rds.stop_db_instance.assert_not_called()

    def test_ecs_describe_failure_is_rejected(self):
        self.ecs.describe_services.return_value = {
            "failures": [{"arn": "sanctuary-api-dev", "reason": "MISSING"}],
            "services": [],
        }
        self.rds.describe_db_instances.return_value = self.database("available")

        with self.assertRaisesRegex(RuntimeError, "ECS describe failed"):
            self.module.handler({"action": "stop"}, None)

        self.ecs.update_service.assert_not_called()
        self.rds.stop_db_instance.assert_not_called()

    def test_wrong_ecs_service_arn_is_rejected(self):
        self.ecs.describe_services.return_value["services"][0]["serviceArn"] = (
            "arn:aws:ecs:us-east-1:160885294528:service/prod/prod"
        )

        with self.assertRaisesRegex(RuntimeError, "Refusing ECS service"):
            self.module.handler({"action": "stop"}, None)

        self.ecs.update_service.assert_not_called()
        self.rds.stop_db_instance.assert_not_called()

    def test_wrong_rds_arn_is_rejected(self):
        self.rds.describe_db_instances.return_value = self.database(
            "available",
            arn="arn:aws:rds:us-east-1:160885294528:db:sanctuary-prod-db",
        )

        with self.assertRaisesRegex(RuntimeError, "Refusing RDS instance"):
            self.module.handler({"action": "stop"}, None)

        self.rds.stop_db_instance.assert_not_called()

    def test_terminal_rds_failure_is_reported(self):
        self.rds.describe_db_instances.return_value = self.database("storage-full")

        with self.assertRaisesRegex(RuntimeError, "terminal failure state"):
            self.module.handler({"action": "stop"}, None)

        self.rds.stop_db_instance.assert_not_called()

    def test_transitional_rds_timeout_is_reported(self):
        self.rds.describe_db_instances.return_value = self.database("starting")

        with patch.object(self.module.time, "monotonic", side_effect=[0, 6]):
            with self.assertRaisesRegex(RuntimeError, "transitional state starting"):
                self.module.handler({"action": "stop"}, None)

        self.rds.stop_db_instance.assert_not_called()

    def test_non_stop_action_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "Only the stop action"):
            self.module.handler({"action": "start"}, None)

        self.module._clients.assert_not_called()


class TemplateSafetyTests(unittest.TestCase):
    def test_schedule_is_disabled_by_default(self):
        text = TEMPLATE.read_text(encoding="utf-8")
        self.assertIn("Default: DISABLED", text)
        self.assertIn("ScheduleExpressionTimezone: America/New_York", text)
        self.assertIn("ScheduleExpression: cron(23 3 * * ? *)", text)
        self.assertIn('Mode: "OFF"', text)

    def test_template_contains_only_exact_dev_targets(self):
        text = TEMPLATE.read_text(encoding="utf-8")
        self.assertNotIn("sanctuary-prod", text)
        self.assertNotIn("sanctuary-uat", text)
        self.assertIn("sanctuary-dev-db", text)
        self.assertIn("service/sanctuary-dev/sanctuary-api-dev", text)

    def test_template_does_not_reserve_account_concurrency(self):
        text = TEMPLATE.read_text(encoding="utf-8")
        self.assertNotIn("ReservedConcurrentExecutions", text)

    def test_log_permissions_use_cloudformation_log_group_arn(self):
        text = TEMPLATE.read_text(encoding="utf-8")
        self.assertIn("Resource: !GetAtt ShutdownLogGroup.Arn", text)
        self.assertNotIn("${ShutdownLogGroup.Arn}:*", text)
        self.assertNotIn("log-stream:*", text)


if __name__ == "__main__":
    unittest.main()
