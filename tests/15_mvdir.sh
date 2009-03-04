mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

mv $MP/two $MP/three

test -e $MP/three/two_only
test -e $MP/three/one_and_two
test -e $MP/three/two_and_one
test -e $MP/three/one/one_and_two
test -e $MP/three/one/two_and_one
test ! -e $MP/three/one_only

test -e $MP/one/one_only
test -e $MP/one/one_and_two
test -e $MP/one/two_and_one
test -e $MP/one/three/one_and_two
test -e $MP/one/three/two_and_one
test ! -e $MP/one/two_only

test ! -d $MP/three/two
test ! -d $MP/one/one

test -d $ORIGIN/three
