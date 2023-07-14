terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

provider "docker" {
  host = "unix:///var/run/docker.sock"
}

resource "docker_network" "kafka" {
  name = "kafka-network"
}

module "zookeeper" {
  source         = "../../terraform/zookeeper"
  count          = local.zookeeper_count
  container_name = "zookeeper-${count.index + 1}"
  network        = docker_network.kafka.name
  server_id      = count.index
  servers        = join(";", [for i in range(local.zookeeper_count) : "zookeeper-${i + 1}:2888:3888"])
}

module "broker" {
  source                 = "../../terraform/kafka"
  count                  = local.kafka_count
  network                = docker_network.kafka.name
  container_name         = "kafka-${count.index + 1}"
  zookeeper_connect_list = module.zookeeper[*].zookeeper_connect
  server_id              = count.index
  depends_on             = [module.zookeeper]
}

module "schema-registry" {
  source                 = "../../terraform/schema-registry"
  kafka_connect_list     = local.broker_address_list
  zookeeper_connect_list = module.zookeeper[*].zookeeper_connect
  depends_on             = [module.zookeeper, module.broker]
  network                = docker_network.kafka.name
}

module "ksqldb-server" {
  source         = "../../terraform/ksqldb-server"
  container_name = "ksqldb-server"
  network        = docker_network.kafka.name
  depends_on     = [module.broker]
}

module "ksqldb-cli" {
  source         = "../../terraform/ksqldb-cli"
  container_name = "ksqldb-cli"
  network        = docker_network.kafka.name
  depends_on     = [module.broker, module.ksqldb-server]
}

module "datasource" {
  source = "./script"
}

resource "null_resource" "create_topic" {
  depends_on = [time_sleep.wait_for_kafka]
  for_each   = toset(["titles", "production_changes"])
  provisioner "local-exec" {
    command = <<-EOF
      docker exec -i kafka-1 \
      kafka-topics \
      --bootstrap-server localhost:9092 \
      --topic ${each.value} \
      --replication-factor 1 \
      --partitions ${local.kafka_count} \
      --create
    EOF
  }
}

resource "docker_container" "datasource" {
  image = module.datasource.image_name
  name  = "datasource"
  env   = [
    "BOOTSTRAP_SERVERS=${tostring(join(",", local.broker_address_list))}",
    "TITLE_COUNT=50",
    "SCHEMA_REGISTRY_URL=http://schema-registry:8081"
  ]
  depends_on = [null_resource.create_topic, module.schema-registry]
  networks_advanced {
    name = docker_network.kafka.name
  }
}


## 컨테이너가 다 뜰 때 까지 대기
resource "time_sleep" "wait_for_kafka" {
  create_duration = "30s"
  depends_on      = [module.broker]
}

locals {
  zookeeper_count     = 3
  kafka_count         = 5
  broker_address_list = module.broker[*].address
}