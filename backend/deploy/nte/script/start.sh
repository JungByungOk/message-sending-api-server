#!/bin/sh
BASE_DIR=""
PROGRAM_LANGUAGE=java
JAVA_HOME=/svc/java
SERVICE_NAME=nte
SERVICE_EXT="jar"
SERVICE_HOME="svc"
SERVICE_BIN_DIR="bin"
JAVA_OPT="-Duser.timezone=UTC \
	  -Djava.security.egd=file:/dev/./urandom \
	  -Dspring.profiles.active=prod \
	  -Xms1024m \
	  -Xmx1024m"
ARGS=$@

# =========================================================
# check the input parameters
# =========================================================
if [ "${ARGS}" = "" ]; then
	echo "Please input the port number 7090 ~ 7093 (nte default 7092)"
	exit 0
fi

# =========================================================
# stop the process (graceful)
# =========================================================
echo "# ========================================================="
echo "# Stop the nte process"
echo "# ========================================================="
$(dirname "$0")/stop.sh

sleep 2

# =========================================================
# start the process
# =========================================================
echo "# ========================================================="
echo "# Start nte Application"
echo "# ========================================================="
for PORT_NUMBER in ${ARGS[@]}
do
	if [ -f "${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT}" ]; then
		nohup ${JAVA_HOME}/bin/java -jar ${JAVA_OPT} -Dserver.port=${PORT_NUMBER} ${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT} > /dev/null 2>&1 &
		echo "nohup ${SERVICE_NAME}.${SERVICE_EXT} port=${PORT_NUMBER} start..."

		# Health check
		echo "Waiting for application to start..."
		RETRIES=0
		MAX_RETRIES=30
		while [ ${RETRIES} -lt ${MAX_RETRIES} ]; do
			HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${PORT_NUMBER}/actuator/health 2>/dev/null)
			if [ "${HTTP_CODE}" = "200" ]; then
				echo "${SERVICE_NAME}.${SERVICE_EXT} is UP on port ${PORT_NUMBER}"
				break
			fi
			RETRIES=$((RETRIES + 1))
			sleep 2
		done

		if [ ${RETRIES} -ge ${MAX_RETRIES} ]; then
			echo "WARNING: Health check timeout after ${MAX_RETRIES} attempts"
		fi
	else
		echo "Not exist binary file : ${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT}"
	fi

	sleep 3
done
