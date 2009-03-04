mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

mv $MP/two/two_only $MP/one/two_only

test ! -e $ORIGIN/two/two_only
test -e $ORIGIN/one/two_only

test -e $MP/two/one_and_two
test -e $MP/two/two_and_one
test ! -e $MP/two/one_only
test ! -d $MP/two/one

test -e $MP/one/two_only
test -e $MP/one/one_only
test -e $MP/one/two_and_one
test -e $MP/one/one_and_two
