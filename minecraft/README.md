# Minecraft Package and import

`
tar -zxvf minecraft-0.1-SNAPSHOT-distribution.tar.gz
cd minecraft-0.1-SNAPSHOT/bin/
./minecraftPackager.sh /Minecraft/ /data/dspace/tmp_data/minecraft/minecraft_filelist.csv /data/dspace/tmp_data/minecraft/output/
/data/dspace/bin/dspace import --add --eperson=dm-admin@kb.dk --collection=1902/20 --source=output/ --mapfile=output/mapfile
/data/dspace/bin/dspace filter-media
/data/dspace/bin/dspace index-discovery
`

Husk at bruge 'screen'...
