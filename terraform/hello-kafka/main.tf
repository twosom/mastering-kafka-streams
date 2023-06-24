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
  count          = local.zookeeper_count
  source         = "../zookeeper"
  container_name = "zookeeper-${count.index + 1}"
  network        = docker_network.kafka.name
  server_id      = count.index
  servers        = join(";", [for i in range(local.zookeeper_count) : "zookeeper-${i + 1}:2888:3888"])
}

module "broker" {
  source                 = "../kafka"
  count                  = local.kafka_count
  network                = docker_network.kafka.name
  container_name         = "kafka-${count.index + 1}"
  zookeeper_connect_list = tolist(module.zookeeper[*].zookeeper_connect)
  server_id              = count.index
  depends_on             = [module.zookeeper]
}

locals {
  zookeeper_count     = 3
  kafka_count         = 5
  broker_address_list = join(",", module.broker[*].address)
}