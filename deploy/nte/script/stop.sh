#!/bin/sh
PROGRAM_LANGUAGE=java
SERVICE_NAME=nte
SERVICE_EXT="jar"
GRACEFUL_TIMEOUT=30

# =========================================================
# stop the process (graceful shutdown)
# =========================================================
echo "# ========================================================="
echo "# Stop the nte process (graceful)"
echo "# ========================================================="
for SERVICE_PID in $(ps -ef | grep ${SERVICE_NAME}.${SERVICE_EXT} | grep ${PROGRAM_LANGUAGE} | grep -v grep | awk '{print $2}')
do
	if [ "${SERVICE_PID}" ]; then
		echo "${SERVICE_NAME}.${SERVICE_EXT}[${SERVICE_PID}] sending SIGTERM..."
		kill -15 "${SERVICE_PID}"

		# Wait for graceful shutdown
		WAITED=0
		while kill -0 "${SERVICE_PID}" 2>/dev/null; do
			if [ ${WAITED} -ge ${GRACEFUL_TIMEOUT} ]; then
				echo "${SERVICE_NAME}.${SERVICE_EXT}[${SERVICE_PID}] graceful timeout (${GRACEFUL_TIMEOUT}s), sending SIGKILL..."
				kill -9 "${SERVICE_PID}"
				break
			fi
			sleep 1
			WAITED=$((WAITED + 1))
		done

		echo "${SERVICE_NAME}.${SERVICE_EXT}[${SERVICE_PID}] stopped."
	else
		echo "${SERVICE_NAME}.${SERVICE_EXT} is NOT running..."
	fi
done
