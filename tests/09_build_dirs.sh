mkdir -p $ORIGIN/one/two
mkdir -p $ORIGIN/two/one

touch $ORIGIN/one/one_only
touch $ORIGIN/two/two_only
touch $ORIGIN/one/two/one_and_two
touch $ORIGIN/two/one/two_and_one

mount

mkdir -p $MP/three/one/two
echo my_string > $MP/three/NEW1
test -e $MP/three/NEW1
echo my_string > $MP/three/one/NEW2
test -e $MP/three/one/NEW2
echo my_string > $MP/three/one/two/NEW3
test -e $MP/three/NEW1
test -e $MP/three/one/NEW2
test -e $MP/three/one/two/NEW3
test -e $MP/one/NEW3
test -e $MP/two/NEW3
test -e $MP/two/NEW3

mkdir -p $MP/testing_getchild/three/folder
touch $MP/testing_getchild/placeholder
touch $MP/testing_getchild/three/file
ls $MP/three | grep -qx file
ls $MP | grep -qx testing_getchild
ls $MP/testing_getchild/three | grep -qx folder
rmdir $MP/testing_getchild/three/folder
