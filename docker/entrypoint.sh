#!/bin/sh
set -eu

target="${DB_TARGET:-local}"

case "$target" in
	local)
		export SPRING_PROFILES_ACTIVE=local
		export SPRING_DATASOURCE_URL="jdbc:postgresql://postgres:5432/cocos"
		export SPRING_DATASOURCE_USERNAME="${LOCAL_DB_USERNAME:-cocos}"
		export SPRING_DATASOURCE_PASSWORD="${LOCAL_DB_PASSWORD:-cocos}"
		echo "DB target: local Postgres from Compose (official seed)"
		;;
	neon)
		export SPRING_PROFILES_ACTIVE=neon
		if [ -z "${SPRING_DATASOURCE_URL:-}" ] || [ -z "${SPRING_DATASOURCE_USERNAME:-}" ] || [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
			echo "DB_TARGET=neon requires SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD" >&2
			exit 1
		fi
		case "$SPRING_DATASOURCE_URL" in
			*@*)
				echo "Put the database user and password in their own variables, not in the JDBC URL" >&2
				exit 1
				;;
			*sslmode=require*|*sslmode=verify-full*|*sslmode=verify-ca*)
				;;
			*)
				echo "The JDBC URL must set sslmode=require" >&2
				exit 1
				;;
		esac
		echo "DB target: Neon (schema is not modified and the seed is not loaded)"
		;;
	*)
		echo "DB_TARGET must be local or neon" >&2
		exit 1
		;;
esac

exec java -jar /app/app.jar
