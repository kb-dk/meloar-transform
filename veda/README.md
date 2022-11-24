# VEDA Package and import

`
tar -zxvf veda-0.1-SNAPSHOT-distribution.tar.gz
cd veda-0.1-SNAPSHOT/bin/
./vedaPackager.sh /data/dspace/tmp_data/veda/input/ /data/dspace/tmp_data/veda/Veda-metadata_jamo_short.csv /data/dspace/tmp_data/veda/output/
/data/dspace/bin/dspace import --add --eperson=dm-admin@kb.dk --collection=1902/20 --source=output/ --mapfile=output/mapfile
/data/dspace/bin/dspace filter-media
/data/dspace/bin/dspace index-discovery
`

Husk at bruge 'screen'...
