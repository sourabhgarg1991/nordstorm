terraform {
  required_version = ">= 1.9, < 2"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5, < 6"
    }
    ccloud = {
      source  = "artifactory.nordstrom.com/proton/ccloud"
      version = ">= 0.12, < 1"
    }
  }
}

provider "aws" {
  region = local.default_region
  default_tags {
    tags = {
      Environment = var.environment
      AppName     = var.env_app_name
      AppId       = var.app_id
    }
  }
}

provider "ccloud" {
  api_endpoint        = local.ccloud_api_endpoint
  oauth_client_id     = var.oauth_client_id
  oauth_client_secret = var.oauth_client_secret
}

#This is configured by the standard pipeline
data "aws_iam_account_alias" "current" {}

# IP prefixes for the NSK internal IPs.  In other words, nodes running pods.
data "aws_ec2_managed_prefix_list" "nsk-internal" {
  name = local.nsk_prefix_list[local.environment]
}

# IP prefixes for the nonprod github runners.
data "aws_ec2_managed_prefix_list" "github-runners" {
  name = local.github_runners_prefix_list[local.environment]
}

# Data source to reference the existing deployment IAM role
data "aws_iam_role" "deployment_role" {
  name = "APP09831_Deployment_Role"
}

# Attach the AmazonRDSFullAccess policy to the existing role
resource "aws_iam_role_policy_attachment" "rds_full_access" {
  count      = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  role       = data.aws_iam_role.deployment_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRDSFullAccess"
}