#!/bin/sh

echo -n "Compiling..."

if ant compile >/dev/null; then
	echo "\rBUILD COMPLETE" 
else
	echo "\rBUILD FAILED"
	exit 1
fi

java -ea intfs.tests.FilesystemTests 2>&1 | tee tests.log

(
cd `dirname $0`

export LOGLEVEL=DEBUG
readonly ORIGIN=`pwd`/test.origin
readonly MP=`pwd`/test.mount
readonly SCRATCH=`pwd`/test.scratch
readonly LOG=io_test.log
readonly LOG2=test-stderr.log
readonly MOUNT="`readlink -f intfs_mount.sh` $ORIGIN $MP"

rm -f $LOG $LOG2

cat > test_helper << MOUNT
#!/bin/bash
$MOUNT 2>&1 | tee -a $LOG >/dev/null & disown
MAX_DELAY_MS=5000
delay=0
while test \`ls -l \$MP | wc -l\` -eq 1; do
	sleep .1
	let delay+=100
	if test \$delay -gt \$MAX_DELAY_MS; then
		echo "mount timeout"
		exit 1
	fi
done
MOUNT
chmod +x test_helper

test `whoami` != root

export ORIGIN MP SCRATCH PATH=".:${PATH}"
ALL_PASSED=true
fusermount -uz $MP 2>/dev/null || true
echo -n "IO integrity: "
rm -rf $ORIGIN $MP $SCRATCH
mkdir -p $ORIGIN $MP $SCRATCH

bash -eux >$LOG 2>$LOG2 << EOF
mkdir -p \$ORIGIN/one/two
mkdir -p \$ORIGIN/two/one

touch \$ORIGIN/two/two_only.baz
touch \$ORIGIN/one/one_only.bar
touch \$ORIGIN/one/two/one_and_two.foo
touch \$ORIGIN/two/one/two_and_one.foo

test_helper

mkdir \$MP/log
dmesg > \$SCRATCH/messages
cp \$SCRATCH/messages \$ORIGIN/TMP_FILE
cp \$ORIGIN/TMP_FILE \$MP/log/messages
diff \$ORIGIN/TMP_FILE \$MP/log/messages

mkdir \$MP/block
A=\$SCRATCH/random-junk
B=\$MP/block/random
C=\$SCRATCH/random-swap

# copy random data
dd if=/dev/urandom of=\$A count=1024 >/dev/null 2>&1
cp \$A \$B
diff \$A \$B

# read single byte
dd if=\$A of=\$A.swp count=1 skip=10 bs=1 >/dev/null 2>&1
dd if=\$B of=\$B.swp count=1 skip=10 bs=1 >/dev/null 2>&1
diff \$A.swp \$B.swp

# write single byte
dd if=/dev/zero of=\$A seek=234 count=1 bs=1 conv=notrunc >/dev/null 2>&1
dd if=/dev/zero of=\$B seek=234 count=1 bs=1 conv=notrunc >/dev/null 2>&1
diff \$A \$B

# write random data
dd if=/dev/urandom of=\$C count=100 bs=1 >/dev/null 2>&1
dd if=\$C of=\$A seek=512 count=100 conv=notrunc bs=1 >/dev/null 2>&1
dd if=\$C of=\$B seek=512 count=100 conv=notrunc bs=1 >/dev/null 2>&1
diff \$A \$B

# read and write from random positions
dd if=\$A of=\$A.swp skip=100 count=100 bs=1 >/dev/null 2>&1
dd if=\$B of=\$B.swp skip=100 count=100 bs=1 >/dev/null 2>&1
diff \$A.swp \$B.swp
dd if=\$A.swp of=\$A seek=20 count=100 skip=3 bs=1 conv=notrunc >/dev/null 2>&1
dd if=\$B.swp of=\$B seek=20 count=100 skip=3 bs=1 conv=notrunc >/dev/null 2>&1
diff \$A \$B

# truncate files
dd if=\$C of=\$A count=12 bs=1 >/dev/null 2>&1
dd if=\$C of=\$B count=12 bs=1 >/dev/null 2>&1
diff \$A \$B

rm \$A.swp \$B.swp
rm \$A \$B \$C
EOF
if [ $? -eq 0 ]; then
	echo "OK"
else
	echo "FAIL"
fi
fusermount -uz $MP

rm -rf $ORIGIN $MP $SCRATCH test_helper $LOG $LOG2
) 2>&1 | tee -a tests.log
