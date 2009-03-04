mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

mkdir -p $MP/three/one/two
touch $MP/three/one/two/FOO 2>/dev/null || true

test -e $MP/three/FOO
test -e $MP/two/FOO
test -e $MP/one/FOO
