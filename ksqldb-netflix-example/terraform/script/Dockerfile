FROM python:3.8.13-slim-bullseye
WORKDIR /root
RUN apt-get update && \
  apt-get install -y --no-install-recommends gcc git libssl-dev g++ make && \
  cd /tmp && git clone https://github.com/edenhill/librdkafka.git && \
  cd librdkafka && git checkout tags/v1.9.0 && \
  ./configure && make && make install && \
  cd ../ && rm -rf librdkafka

ADD main.py .
ADD requirements.txt .
RUN pip3 install -r requirements.txt
CMD ["python3", "main.py"]