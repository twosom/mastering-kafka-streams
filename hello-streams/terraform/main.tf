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
  zookeeper_connect_list = tolist(module.zookeeper[*].zookeeper_connect)
  server_id              = count.index
  depends_on             = [module.zookeeper]
}

## 컨테이너가 다 뜰 때 까지 대기
resource "time_sleep" "wait_for_kafka" {
  create_duration = "10s"
  depends_on      = [module.broker]
}

## 샘플 토픽 작성
resource "null_resource" "create_topic" {
  depends_on = [time_sleep.wait_for_kafka]
  provisioner "local-exec" {
    command = "docker exec -i kafka-1 kafka-topics --bootstrap-server localhost:9092 --topic users --create"
  }
}

locals {
  zookeeper_count     = 3
  kafka_count         = 5
  broker_address_list = join(",", module.broker[*].address)
}

output "broker-address" {
  value = module.broker[*].broker-address
}