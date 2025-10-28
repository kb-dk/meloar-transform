# Sag-10112171 — Nyt datasæt til LOAR Statslige Digitale Publikationer /Government Digital Publications

Vi vil gerne have lagt et nyt datasæt i LOAR under Open Digital Collections -> Texts:
https://loar.kb.dk/communities/53c571c0-f92a-4a8f-afad-10f538bcbe7b
Titel:  Statslige Digitale Publikationer /Government Digital Publications

I første omgang bedes det kun  være en test opsætning som Lars, Madeleine og Tue kan tilgå.
Dvs. på Stage.

Filerne er for store til at uploade på web-siden, derfor følgende:
Eksporter dette item (uden bitstream)
`/data/dspace/bin/dspace export -t ITEM -i 1902/50 -d /data/dspace/exports/ -n 3 -x`

Kopier
`scp -r dspace@dspace-stage.statsbiblioteket.dk:/data/dspace/exports/3 .`

Opret contents file og kopier ny Simple Archive Format (SAF)
`baj@baj-fedora:~/Projects/meloar-transform/statpub/src/main$ scp -r resources/3 dspace@dspace-stage.statsbiblioteket.dk:/data/dspace/temp/statpub/`

Brug itemupdate -addbitstream
`/data/dspace/bin/dspace itemupdate -A -e dm-admin@kb.dk -s /data/dspace/temp/statpub -t`

Husk tmux eller lignende, hvis det altså er installeret!

Mindre rettelser på stage
Export fra Stage igen (inklusiv bitstrems). Husk at bruge --migrate.
    `/data/dspace/bin/dspace export -t ITEM -i 1902/50 -d /data/dspace/exports/ -n 5 --migrate`

Importer på Prod
`/data/dspace/bin/dspace import -a -e dm-admin@kb.dk -c 1902/49116 -s /data/dspace/imports/statpub/ -m mapfile`

