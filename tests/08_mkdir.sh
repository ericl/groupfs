mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

mkdir -p $MP/red
mkdir -p $MP/blue
mkdir -p $MP/yellow
mv $MP/red $MP/orange
mv $MP/yellow $MP/purple
touch $MP/purple/x
touch $MP/orange/RED_FILE
rmdir $MP/blue

test ! -d $MP/yellow
test -e $MP/purple/x
test -e $MP/orange/RED_FILE
test -e $ORIGIN/orange/RED_FILE
test ! -e $MP/red/RED_FILE
test ! -e $ORIGIN/red/RED_FILE

mkdir -p $MP/asdf
test ! -d $ORIGIN/asdf
test ! -e $ORIGIN/asdf/.hidden
