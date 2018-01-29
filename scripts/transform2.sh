#!/bin/bash
#
# Transform into json file
#
INPUTDIR=$1
OUTPUTDIR=$2


# ProjectPath is one directory up from script bin directory.
PROJECTPATH=$( cd $(dirname $0)/.. ; pwd -P )

CLASSPATH="${PROJECTPATH}/PatentDocument/target/*:${PROJECTPATH}/PatentDocument/target/dependency-jars/*"

JAVA="java -cp ${CLASSPATH} -Dlog4j.configuration=file:${PROJECTPATH}/conf/log4j.properties"

${JAVA} gov.uspto.patent.TransformerCli2 --input=${INPUTDIR} --outdir=${OUTPUTDIR} --outBulk=false --prettyPrint=false



