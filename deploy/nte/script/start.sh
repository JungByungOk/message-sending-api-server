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
# Get the arguments
ARGS=$@

# =========================================================
# check the input parameters
# =========================================================
if [ "${ARGS}" = "" ]; then
	echo "Please input the port number 7090 ~ 7093 (nte default 7092)"
	exit 0
fi

# =========================================================
# stop the process
# =========================================================
echo "# ========================================================="
echo "# Stop the ntc process"
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

# =========================================================
# start the process
# =========================================================
echo "# ========================================================="
echo "# Start ntc Application"
echo "# ========================================================="
for PORT_NUMBER in ${ARGS[@]}
do
	if [ -f "${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT}" ]; then
		nohup ${JAVA_HOME}/bin/java -jar ${JAVA_OPT} -Dserver.port=${PORT_NUMBER} ${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT} > /dev/null 2>&1 &
		echo "nohup ${SERVICE_NAME}.${SERVICE_EXT} port=${PORT_NUMBER} start..."
	else
		echo "Not exist binary file : ${BASE_DIR}/${SERVICE_HOME}/${SERVICE_NAME}/${SERVICE_BIN_DIR}/${SERVICE_NAME}.${SERVICE_EXT}"
	fi

	sleep 3
done	
