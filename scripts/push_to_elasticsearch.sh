#!/bin/bash
#
# Push each json file to elasticsearch
#
ESURL=$1
INPUTDIR=$2

cd $INPUTDIR
for filename in *.json;
do
    y=${filename%.json}
    id=${y##*/}

    echo ${id}
    echo ${filename}

    curl -XPOST "${ESURL}/${id}" -d @${filename}

done
