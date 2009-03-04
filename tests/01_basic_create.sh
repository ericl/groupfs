mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one
mkdir -p $ORIGIN/three/one

touch $ORIGIN/three/one/three
touch $ORIGIN/three/one/three2
touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

OLD=`tree $ORIGIN | openssl md5`

mount

test -e $MP/two/two_only
test -e $MP/two/one_and_two
test -e $MP/two/two_and_one
test -e $MP/two/one/one_and_two
test -e $MP/two/one/two_and_one
test ! -e $MP/two/one_only

test -e $MP/one/one_only
test -e $MP/one/one_and_two
test -e $MP/one/two_and_one
test -e $MP/one/two/one_and_two
test -e $MP/one/two/two_and_one
test ! -e $MP/one/two_only

test ! -d $MP/two/two
test ! -d $MP/one/one

test $OLD = `tree $ORIGIN | openssl md5`

test `ls -1a $MP/one | wc -l` = 7

test -d $MP/one/two
rmdir $MP/one # allowed to do this
test ! -d $MP/one
