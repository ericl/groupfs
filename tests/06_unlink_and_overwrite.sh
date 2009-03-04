mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

echo my_string > $MP/two/one/probe
rm $MP/two/one/one_and_two
mv $MP/two/one/probe $MP/two/one/one_and_two

test ! -e $MP/two/one/probe
test `cat $MP/two/one/one_and_two` = my_string
test `cat $MP/one/two/one_and_two` = my_string

test `cat $ORIGIN/two/one/one_and_two` = my_string
test -e $ORIGIN/*one_and_two*
