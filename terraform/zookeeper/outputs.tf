output "zookeeper_connect" {
  value = "${docker_container.zookeeper.hostname}:2181"
}