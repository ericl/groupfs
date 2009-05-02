#!/bin/sh

echo -n "Compiling..."

if ant compile >/dev/null; then
	echo "\rBUILD COMPLETE" 
else
	echo "\rBUILD FAILED"
	exit 1
fi

java -ea queryfs.tests.FilesystemTests 2>&1 | tee tests.log
