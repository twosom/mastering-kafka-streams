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

module "datasource_image" {
  source = "./script"
}

## 컨테이너가 다 뜰 때 까지 대기
resource "time_sleep" "wait_for_kafka" {
  create_duration = "10s"
  depends_on      = [module.broker]
}

## 트윗 토픽 작성
resource "null_resource" "create_topic" {
  for_each   = toset(["score-events", "players", "products", "high-scores"])
  depends_on = [time_sleep.wait_for_kafka]
  provisioner "local-exec" {
    command = <<-EOF
      docker exec -i kafka-1 \
      kafka-topics \
      --bootstrap-server localhost:9092 \
      --topic ${each.value} \
      --partitions ${local.kafka_count} \
      --replication-factor 1 \
      --create
    EOF
  }
}

resource "docker_container" "datasource_container" {
  image      = module.datasource_image.image_name
  name       = "datasource"
  depends_on = [null_resource.create_topic]
  env        = [
    "BOOTSTRAP_SERVERS=${tostring(join(",", local.broker_address_list))}",
    "TOPIC_SEND_INTERVAL=0.1"
  ]
  networks_advanced {
    name = docker_network.kafka.name
  }
}

locals {
  zookeeper_count     = 3
  kafka_count         = 5
  broker_address_list = module.broker[*].address
}

output "broker-address" {
  value = module.broker[*].broker-address
}