mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/frog/pi
mkdir -p $ORIGIN/pi

touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/frog/pi/frog_and_pi
touch $ORIGIN/frog/frog_only

mount

test -d $MP/one
test ! -d $MP/one/two
test -e $MP/one/one_and_two
mkdir $MP/one/two
rmdir $MP/one/two
test -e $MP/two/one_and_two

test -e $MP/frog/pi/frog_and_pi
rmdir $MP/frog
test ! -d $MP/frog
