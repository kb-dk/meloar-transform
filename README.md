# MELOAR transform
This repository is part of the MEdiestream for LOAR innovation week project. 
It can be used to convert metadata from [https://www.kulturarv.dk/ffrepox/OAIHandler]
to items including pdf "beretninger" and metadata to a simple archive format, which can 
be ingested into LOAR [https://loar.kb.dk/].

## Transforming data
The tool can be run using this command line:

`java -jar meloar-transform-packager-0.1-SNAPSHOT.jar /home/Projects/meloar-solr/ff_slks /mnt/data/data/meloar > log`

assuming that the harvested metadata is located in `/home/Projects/meloar-solr/ff_slks` and you have enough space in 
`/mnt/data/data/meloar`.

## Ingesting data
The items ready for ingest are now in `/mnt/data/data/meloar` and assuming you are on 
the LOAR production platform, you can ingest them using this command:

`/data/dspace/bin/dspace import -a -e dm-admin@kb.dk -c 1902/401 -s /mnt/data/data/meloar -m /mnt/data/data/meloar/mapfile`

To make the newly ingested pdf's searchable in LOAR, you need to "filter media",

`/data/dspace/bin/dspace filter-media`

update the discovery index,

`/data/dspace/bin/dspace index-discovery`

and update the oai index

`/data/dspace/bin/dspace oai import -o`
