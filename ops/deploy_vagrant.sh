#!/bin/bash
ssh vagrant@192.168.50.81 <<EOF
  sudo docker stop freecoin || echo 'Failed to stop freecoin container'
  sudo docker rm freecoin || echo 'Failed to remove freecoin container'
  sudo docker run -d -v /var/freecoin/target:/var/freecoin \
                  -p 5000:8000 \
                  --name freecoin \
                  --link mongo:mongo "--env-file=/var/freecoin/config/freecoin.env" \
                  java:8 bash \
                  -c 'java -jar /var/freecoin/*standalone.jar'
EOF
