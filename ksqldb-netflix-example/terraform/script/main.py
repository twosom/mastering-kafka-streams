import json
import os
import random
from time import sleep

from faker import Faker
from kafka_schema_registry import prepare_producer

faker = Faker()

servers = os.environ['BOOTSTRAP_SERVERS']
title_count = int(os.environ['TITLE_COUNT'])

# Avro 스키마 정의
production_changes_schema = {
    "type": "record",
    "name": "ProductionChange",
    "fields": [
        {"name": "uuid", "type": "string"},
        {"name": "title_id", "type": "string"},
        {"name": "change_type", "type": "string"},
        {"name": "before", "type": {
            "type": "record",
            "name": "BeforeChange",
            "fields": [
                {"name": "season_id", "type": "int"},
                {"name": "episode_count", "type": "int"}
            ]
        }},
        {"name": "after", "type": {
            "type": "record",
            "name": "AfterChange",
            "fields": [
                {"name": "season_id", "type": "int"},
                {"name": "episode_count", "type": "int"}
            ]
        }},
        {"name": "created_at", "type": "string"}
    ]
}

# Define Avro schema for 'titles' topic
titles_schema = {
    "type": "record",
    "name": "Title",
    "fields": [
        {"name": "id", "type": "int"},
        {"name": "title", "type": "string"},
        {"name": "on_schedule", "type": "boolean"}
    ]
}

titles_producer = prepare_producer(
    servers.split(','),
    f'http://schema-registry:8081',
    'titles',
    5,
    1,
    value_schema=titles_schema
)

titles_producer.config['key_serializer'] = lambda x: json.dumps(x).encode('utf-8')

production_changes_producer = prepare_producer(
    servers.split(','),
    f'http://schema-registry:8081',
    'production_changes',
    5,
    1,
    value_schema=production_changes_schema
)
production_changes_producer.config['key_serializer'] = lambda x: json.dumps(x).encode('utf-8')


def generate_random_data(title_id):
    title = faker.catch_phrase()
    on_schedule = random.choice([True, False])
    return {
        'id': title_id,
        'title': title,
        'on_schedule': on_schedule
    }


def generate_change_data(data):
    uuid = faker.uuid4()
    title_id = data['id']
    change_type = random.choice(['season_length', 'title', 'genre'])
    before = {
        'season_id': title_id,
        'episode_count': random.randint(6, 20)
    }
    after = {
        'season_id': title_id,
        'episode_count': random.randint(6, 20)
    }
    created_at = faker.date_time_this_month().strftime("%Y-%m-%d %H:%M:%S")

    return {
        'uuid': uuid,
        'title_id': title_id.__str__(),
        'change_type': change_type,
        'before': before,
        'after': after,
        'created_at': created_at
    }


title_list = []
for i in range(title_count):
    data = generate_random_data(i)
    titles_producer.send('titles', key=i, value=data)
    title_list.append(data)

while True:
    for title in title_list:
        change_data = generate_change_data(title)
        production_changes_producer.send('production_changes', key=change_data['title_id'], value=change_data)
        sleep(1.0 / title_count)
