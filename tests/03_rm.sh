mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

rmdir $MP/two

test ! -d $MP/two

test ! -d $MP/one/two
test -e $MP/one/one_only
test -e $MP/one/two_and_one
test -e $MP/one/one_and_two
