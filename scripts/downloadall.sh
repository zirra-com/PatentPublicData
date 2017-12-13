#!/bin/bash
#
# Download USPTO Weekly Bulk Patent Dumps
#

TYPE=$1


# ProjectPath is one directory up from script bin directory.
PROJECTPATH=$( cd $(dirname $0)/.. ; pwd -P )

CLASSPATH="${PROJECTPATH}/BulkDownloader/target/*:${PROJECTPATH}/BulkDownloader/target/dependency-jars/*"

echo $CLASSPATH
JAVA="java -cp ${CLASSPATH} -Dlog4j.configuration=file:${PROJECTPATH}/conf/log4j.properties"

OUTPUTDIR=${PROJECTPATH}/download/$TYPE
mkdir -p $OUTPUTDIR

#
# type: [application, grant, gazette]    requited;  patent document type
# outdir    directory to download to.
#
${JAVA} gov.uspto.bulkdata.cli2.BulkData --type $TYPE --outdir="$OUTPUTDIR"
