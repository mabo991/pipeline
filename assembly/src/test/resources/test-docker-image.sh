#!/usr/bin/env bash

# This test starts the pipeline inside a Docker container and then
# starts the dtbook-to-epub3 script using the cli inside a second
# container

set -e
cd $(dirname "$0")

# download test DTBook from https://github.com/daisy/pipeline-samples
DATA_DIR=../../../target/test-docker/data
mkdir -p $DATA_DIR
DTBOOK=hauy_valid.xml
cp $DTBOOK $DATA_DIR
cd $DATA_DIR
DATA=$(basename $DTBOOK).zip
rm -f $DATA
cd $DTBOOK
for f in \
    dtbook.2005.basic.css \
    valentin.jpg
do
    curl https://raw.githubusercontent.com/daisy/pipeline-samples/10bbb8e/dtbook/$f 2>/dev/null >$f
    zip $DATA $f
done

CLIENTKEY=clientid
CLIENTSECRET=sekret
MOUNT_POINT=/mnt

# run the pipeline
docker run --name pipeline --detach \
       -e PIPELINE2_WS_HOST=0.0.0.0 \
       -e PIPELINE2_WS_AUTHENTICATION=true \
       -e PIPELINE2_WS_AUTHENTICATION_KEY=$CLIENTKEY \
       -e PIPELINE2_WS_AUTHENTICATION_SECRET=$CLIENTSECRET \
       -p 8181:8181 daisyorg/pipeline-assembly

# wait for the pipeline to start
sleep 5
tries=10
while ! curl localhost:8181/ws/alive >/dev/null 2>/dev/null; do
    if [[ $tries > 0 ]]; then
        echo "Waiting for web service to be up..." >&2
        sleep 2
        (( tries-- ))
    else
        docker logs pipeline
        docker stop pipeline
        docker rm pipeline
        exit 1
    fi
done

# run the cli
if ! docker run --name cli --rm -it --link pipeline \
            --entrypoint /opt/daisy-pipeline2/cli/dp2 \
            --volume="$(pwd):$MOUNT_POINT:rw" \
            daisyorg/pipeline-assembly \
            --host http://pipeline \
            --starting false \
            --client_key $CLIENTKEY \
            --client_secret $CLIENTSECRET \
            celia:dtbook-to-pef --source $DTBOOK --output $MOUNT_POINT --data $MOUNT_POINT/$DATA --persistent \
                                --stylesheet "http://www.daisy.org/pipeline/modules/braille/dtbook-to-pef/xsl/volume-breaking.xsl \
                                              http://www.celia.fi/pipeline/modules/braille/default.scss \
                                              http://www.daisy.org/pipeline/modules/braille/dtbook-to-pef/css/volume-breaking.scss" \
                                --maximum-number-of-sheets 50;
then
    docker logs pipeline
    docker stop pipeline
    docker rm pipeline
    exit 1
fi

docker stop pipeline
docker rm pipeline

