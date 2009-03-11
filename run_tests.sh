#!/bin/bash -u

ABORT_ON_ERROR=${ABORT_ON_ERROR-false}

cd `dirname $0`

export LOGLEVEL=DEBUG
readonly ORIGIN=`pwd`/test.origin
readonly MP=`pwd`/test.mount
readonly SCRATCH=`pwd`/test.scratch
readonly LOG=test.log
readonly LOG2=test-stderr.log
readonly MOUNT="`readlink -f path_mount.sh` $ORIGIN $MP"

rm -f $LOG

echo -n "Compiling..."
if ant compile >/dev/null 2>&1; then
	echo -e "\rBUILD SUCCESSFUL"
else
	echo -e "\rBUILD FAILED"
	exit 1
fi

cat > mount << MOUNT
#!/bin/bash
$MOUNT 2>&1 | tee -a $LOG >/dev/null & disown
MAX_DELAY_MS=5000
delay=0
while df $MP | grep '/dev/' >&2; do
	sleep .1
	let delay+=100
	if test \$delay -gt \$MAX_DELAY_MS; then
		echo "mount timeout"
		exit 1
	fi
done
MOUNT
chmod +x mount

test `whoami` != root

export ORIGIN MP SCRATCH PATH=".:${PATH}"
ALL_PASSED=true
fusermount -uz $MP 2>/dev/null || true
for test in tests/*; do
	[ ! -x $test ] && continue
	echo -n "Running $test... "
	CODE=1
	tries=0
	while [ $tries -lt 5 ] && [ $CODE != 0 ]; do
		fusermount -uzq $MP
		let tries++
		rm -rf $ORIGIN $MP $SCRATCH
		mkdir -p $ORIGIN $MP $SCRATCH
		bash -eux $test 2>$LOG2
		CODE=$?
	done
	if [ $CODE -eq 0 ]; then
		echo "ok"
	else
		echo "FAIL"
		if $ABORT_ON_ERROR; then
			tail $LOG2
			exit 1
		fi
		ALL_PASSED=false
	fi
done
fusermount -u $MP

rm -rf $ORIGIN $MP $SCRATCH mount
if $ALL_PASSED; then
	echo 'All scripted tests passed.'
	exit 0
else
	echo 'One or more tests FAILED.'
	exit 1
fi
