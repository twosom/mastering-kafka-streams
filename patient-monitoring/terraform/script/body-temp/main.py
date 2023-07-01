import json
import os
import random
import time

from kafka import KafkaProducer

servers = os.environ['BOOTSTRAP_SERVERS']
id = os.environ['PATIENT_ID']

print(servers)
producer = KafkaProducer(
    bootstrap_servers=servers,
    key_serializer=lambda x: json.dumps(x).encode('utf-8'),
    value_serializer=lambda x: json.dumps(x).encode('utf-8')
)


def generate_random_data():
    timestamp = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    if random.random() < 0.01:
        temperature = round(random.uniform(100.5, 110), 1)
    else:
        temperature = round(random.uniform(90.0, 100.4), 1)
    unit = "F"
    return {
        "timestamp": timestamp,
        "temperature": temperature,
        "unit": unit
    }


while True:
    data = generate_random_data()
    producer.send('body-temp-events', key=int(id), value=data)
    print(data)
    time.sleep(1)  # 5초마다 데이터 생성 및 전송
