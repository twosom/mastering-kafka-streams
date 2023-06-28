terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "3.0.2"
    }
  }
}

resource "docker_image" "datasource" {
  name = "datasource"
  build {
    context = path.module
    tag = ["datasource:default"]
  }
}