terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}


resource "docker_container" "elasticsearch" {
  image = "docker.elastic.co/elasticsearch/elasticsearch:7.16.0-arm64"
  name  = "elasticsearch"
  env   = [
    "discovery.type=single-node"
  ]

  networks_advanced {
    name = var.network
  }
}