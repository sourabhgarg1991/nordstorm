resource "aws_iam_role" "k8s_s3_role" {
  count               = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  name                = "fin-data-integration-k8s-access-role-${local.environment}"
  managed_policy_arns = ["arn:aws:iam::aws:policy/AmazonS3FullAccess"]
  assume_role_policy = jsonencode(
    {
      "Version" : "2012-10-17",
      "Statement" : [
        {
          "Effect" : "Allow",
          "Principal" : {
            "Federated" : "arn:aws:iam::${local.aws_account_id[local.environment]}:oidc-provider/oidc.eks.us-west-2.amazonaws.com/id/${local.shared_oidc_hash[local.environment]}"
          },
          "Action" : "sts:AssumeRoleWithWebIdentity",
          "Condition" : {
            "StringLike" : {
              "oidc.eks.us-west-2.amazonaws.com/id/${local.shared_oidc_hash[local.environment]}:sub" : "system:serviceaccount:app09831:*"
            }
          }
        }
      ]
    }
  )
  inline_policy {
    name = "rds_policy"
    policy = jsonencode(
      {
        "Version" : "2012-10-17",
        "Statement" : [
          {
            "Effect" : "Allow",
            "Action" : [
              "rds-db:connect",
              "rds-db:*"
            ],
            "Resource" : "arn:aws:rds-db:${local.default_region}:${local.aws_account_id[local.environment]}:dbuser:${local.db.cluster_name.all_clusters}/${local.db.username.all_users}"
          }
        ]
      }
    )
  }
  inline_policy {
    name = "erp_financial_s3_put_object_policy"
    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Effect   = "Allow",
          Action   = "s3:PutObject",
          Resource = local.erp_s3_bucket_name
        }
      ]
    })
  }
}
