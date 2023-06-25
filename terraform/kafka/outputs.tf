output "address" {
  value = "${var.container_name}:9092"
}

output "broker-address" {
  value = "localhost:${local.broker_port}"
}