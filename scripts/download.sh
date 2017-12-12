#!/bin/bash
#
# Download USPTO Weekly Bulk Patent Dumps
#

DATE=$1


# ProjectPath is one directory up from script bin directory.
PROJECTPATH=$( cd $(dirname $0)/.. ; pwd -P )

CLASSPATH="${PROJECTPATH}/BulkDownloader/target/*:${PROJECTPATH}/BulkDownloader/target/dependency-jars/*"

JAVA="java -cp ${CLASSPATH} -Dlog4j.configuration=file:${PROJECTPATH}/conf/log4j.properties"

#
# type: [application, grant, gazette]    requited;  patent document type
# date     required; 20140101-20161231
# limit     download limit
# skip      skip over limit
# async     Async Downloads
# filename  specific bulk file name to download
# outdir    directory to download to.
#
${JAVA} gov.uspto.bulkdata.cli2.BulkData --type application --date $DATE --outdir="${PROJECTPATH}/download"
