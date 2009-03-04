mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

echo my_string > $MP/two/one/probe
test `cat $MP/two/one/probe` = my_string
test `cat $MP/two/probe` = my_string
test `cat $MP/one/probe` = my_string
test `cat $ORIGIN/two/one/probe` = my_string
