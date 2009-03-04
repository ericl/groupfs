mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

echo my_string > $MP/two/one/two_and_one
mv $MP/two/one/two_and_one $MP/two/one/TWO_AND_ONE

test ! -e $MP/two/two_and_one
test ! -e $MP/one/two/two_and_one
test ! -e $MP/two/one/two_and_one
test -e $MP/two/TWO_AND_ONE
test -e $MP/two/one/TWO_AND_ONE
test -e $MP/one/two/TWO_AND_ONE
test `cat $MP/two/TWO_AND_ONE` = my_string
