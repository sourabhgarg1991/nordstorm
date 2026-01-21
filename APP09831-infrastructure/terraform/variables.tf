variable "app_id" {
  type        = string
  description = "The NERDS ID of the application being deployed"
}
variable "app_name" {
  type        = string
  description = "The name of the application"
}
variable "environment" {
  description = "Specifies the environment"
  type        = string
}
variable "env_app_name" {
  description = "Specifies the environment-based app name"
  type        = string
}
variable "aurora_database_name" {
  type        = string
  description = "The name of the database to create when the DB cluster is created"
}
variable "aurora_master_username" {
  description = "Specifies the database admin account user name"
  type        = string
}
variable "aurora_master_password" {
  description = "Specifies the database admin account password"
  type        = string
  sensitive   = true
}
variable "rds_cidr_blocks" {
  type = map(string)
  default = {
    "10.0.0.0/8"      = "Nordstrom domain",
    "161.181.53.0/24" = "vpn access"
  }
}
variable "oauth_client_id" {
  type        = string
  description = "OAuth client ID to get access to the Proton."
}
variable "oauth_client_secret" {
  type        = string
  sensitive   = true
  description = "OAuth secret to get access to the Proton."
}
