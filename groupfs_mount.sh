#!/bin/bash

USAGE="Usage: $0 [-o options] /origin/dir /mount/point"

DEFOPTS="-o rw"

while [ $# -gt 0 ]; do
	case "$1" in
		-o)
			shift
			OPTIONS="-o $1"
		;;
		-h|--help)
			echo "$USAGE"
			exit 1
		;;
		*)
			ORIGIN="$MOUNTPOINT"
			MOUNTPOINT="$1"
		;;
	esac
	shift
done

mkdir -p "$MOUNTPOINT"

if [ ! -d "$ORIGIN" ] || [ ! -d "$MOUNTPOINT" ]; then
	echo "$USAGE"
	exit 1
fi

clean_exit() {
	fusermount -uz "$MOUNTPOINT"
	rmdir "$MOUNTPOINT" 2>/dev/null
	exit ${1-0}
}

trap "clean_exit 1" SIGINT SIGTERM SIGHUP

java -Dorg.apache.commons.logging.Log=fuse.logging.FuseLog \
     -Dfuse.logging.level=${LOGLEVEL-INFO} -ea \
     groupfs.Filesystem ${OPTIONS-$DEFOPTS} -f -s "$MOUNTPOINT" "$ORIGIN"

clean_exit
