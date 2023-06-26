FROM python:3.9-alpine
WORKDIR /root
ADD main.py .
ADD requirements.txt .
RUN pip3 install -r requirements.txt
CMD ["python3", "main.py"]