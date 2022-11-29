#!/bin/sh
PROGRAM_LANGUAGE=java
SERVICE_NAME=nte
SERVICE_EXT="jar"

# =========================================================
# stop the process
# =========================================================
echo "# ========================================================="
echo "# Stop the nte process"
echo "# ========================================================="
for SERVICE_PID in $(ps -ef | grep ${SERVICE_NAME}.${SERVICE_EXT} | grep ${PROGRAM_LANGUAGE} | grep -v grep | awk '{print $2}')
do
	if [ "${SERVICE_PID}" ]; then
		echo "${SERVICE_NAME}.${SERVICE_EXT}[${SERVICE_PID}] stopping..."
		kill -9 "${SERVICE_PID}"
		echo "${SERVICE_NAME}.${SERVICE_EXT}[${SERVICE_PID}] stop..."
		sleep 5
	else
		echo "${SERVICE_NAME}.${SERVICE_EXT} is NOT running..."
	fi

	sleep 3
done
