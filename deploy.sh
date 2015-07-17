lein do clean, uberjar
DIR=deploy_assets
TAR=deploy.tar.gz
VERSION=0.2.0-SNAPSHOT
mkdir -p $DIR
cp target/freecoin-$VERSION-standalone.jar $DIR
cp -r resources/public $DIR
cp -r init-script $DIR
tar -cvzf $TAR $DIR
scp -F $CONFIG_FILE $TAR dob_vm:~
ssh -F $CONFIG_FILE dob_vm "tar -xvzf $TAR; cd $DIR; sudo bash init-script/remote_start_freecoin.sh"
rm -rf $DIR
lein do clean
