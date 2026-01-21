##################################################################################
# SECURITY GROUP
##################################################################################
resource "aws_security_group" "rds_security_group" {
  name   = "rds-nsk-${var.env_app_name}"
  vpc_id = local.existing_vpc_ids[local.environment]
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "rds_ingress_cidr_blocks" {
  for_each          = var.rds_cidr_blocks
  description       = each.value
  type              = "ingress"
  from_port         = local.aurora_postgresql_port
  to_port           = local.aurora_postgresql_port
  protocol          = "tcp"
  cidr_blocks       = [each.key]
  security_group_id = aws_security_group.rds_security_group.id
}

resource "aws_security_group_rule" "rds_ingress_managed_prefixes" {
  description       = "Managed Prefixes for NSK Clusters"
  type              = "ingress"
  from_port         = local.aurora_postgresql_port
  to_port           = local.aurora_postgresql_port
  protocol          = "tcp"
  prefix_list_ids   = [data.aws_ec2_managed_prefix_list.nsk-internal.id, data.aws_ec2_managed_prefix_list.github-runners.id]
  security_group_id = aws_security_group.rds_security_group.id
}

resource "aws_security_group_rule" "rds_security_group_rule_egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.rds_security_group.id
}

##################################################################################
# DB SUBNET GROUP
##################################################################################
resource "aws_db_subnet_group" "db_subnet_group" {
  count       = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  name_prefix = local.aurora_postgresql_name_prefix
  description = "RDS Subnet group with internal subnets for data-integration Database"
  subnet_ids  = local.existing_subnet_ids[local.environment]
  tags        = local.aws_resource_tags
}

##################################################################################
# DB PARAMETERS
##################################################################################
resource "aws_db_parameter_group" "db_parameters_group" {
  count       = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  name_prefix = local.aurora_postgresql_name_prefix
  description = "Database parameter group for data-integration Database"
  family      = local.aurora_postgresql_family
  tags        = local.aws_resource_tags
}

##################################################################################
#  RDS AURORA DATABASE CLUSTER
##################################################################################
resource "aws_rds_cluster" "data_integration_aurora_cluster" {
  count                               = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  database_name                       = lower(var.aurora_database_name)
  cluster_identifier                  = "fin-data-integration-cluster-${local.environment}"
  engine                              = local.aurora_postgresql_engine
  engine_version                      = local.aurora_postgresql_engine_version
  port                                = local.aurora_postgresql_port
  master_username                     = lower(var.aurora_master_username)
  master_password                     = var.aurora_master_password
  iam_database_authentication_enabled = local.iam_aurora_postgresql_auth_enabled
  db_subnet_group_name                = aws_db_subnet_group.db_subnet_group[count.index].name
  final_snapshot_identifier           = "data-integration-${local.environment}-${count.index}-final"
  skip_final_snapshot                 = local.aurora_postgresql_skip_final_snapshot
  deletion_protection                 = local.aurora_postgresql_deletion_protection
  backup_retention_period             = local.aurora_postgresql_backup_retention_period
  tags                                = local.aws_resource_tags
  vpc_security_group_ids              = [aws_security_group.rds_security_group.id, local.existing_security_group_ids[local.environment][0]]
  availability_zones                  = local.default_region_availability_zones
  storage_encrypted                   = local.aurora_postgresql_storage_encrypted
  apply_immediately                   = local.aurora_postgresql_apply_immediately
  lifecycle {
    ignore_changes = [engine_version]
  }
}

##################################################################################
# RDS AURORA DATABASE INSTANCES
##################################################################################
resource "aws_rds_cluster_instance" "data_integration_db_instance1" {
  count                      = local.is_nonprod_or_prod ? 1 : 0 # created only in nonprod and prod environment
  identifier                 = "fin-data-integration-${local.environment}-instance1"
  cluster_identifier         = aws_rds_cluster.data_integration_aurora_cluster[count.index].id
  instance_class             = local.aurora_postgresql_instance_class
  engine                     = local.aurora_postgresql_engine
  engine_version             = local.aurora_postgresql_engine_version
  tags                       = local.aws_resource_tags
  auto_minor_version_upgrade = local.auto_minor_version_upgrade
  db_parameter_group_name    = aws_db_parameter_group.db_parameters_group[count.index].name
  db_subnet_group_name       = aws_db_subnet_group.db_subnet_group[count.index].name
}

resource "aws_rds_cluster_instance" "data_integration_db_instance2" {
  count                      = local.is_prod ? 1 : 0 # created only in prod environment
  identifier                 = "fin-data-integration-${local.environment}-instance2"
  cluster_identifier         = aws_rds_cluster.data_integration_aurora_cluster[count.index].id
  instance_class             = local.aurora_postgresql_instance_class
  engine                     = local.aurora_postgresql_engine
  engine_version             = local.aurora_postgresql_engine_version
  tags                       = local.aws_resource_tags
  auto_minor_version_upgrade = local.auto_minor_version_upgrade
  db_parameter_group_name    = aws_db_parameter_group.db_parameters_group[count.index].name
  db_subnet_group_name       = aws_db_subnet_group.db_subnet_group[count.index].name
}

