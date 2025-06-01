# OCR-scanningsfiler af ældre danske aviser

Med aviserne forholder det sig således, at der i Mediestream gives adgang fra 99 år og bagud. Pr. 1/1-2023 er der 
således givet adgang til årgang 1923. Betingelserne, som kort fortalt er ”til privat brug”, som denne adgang gives 
under, medfølger når man downloader en pdf-kopi af en given avis.

Når det gælder LOAR – og altså komplette årgange af aviserne i Mediestream til download, herunder tekstkorpus – hedder 
den rullende grænse 140 år. I år ville vil således kunne give adgang til årgang 1882, men der er kun adgang til 1881
for fulde batches og kun 1877 for tekstkorpus pr. april 2013. Se https://loar.kb.dk/handle/1902/157

I vedligeholdelsesperioden maj 2023 opdaterer vi tekstkorpus til og med årgang 1882. Det er Sag-10100117
https://servicedesk.kb.dk/otrs/index.pl?Action=AgentTicketZoom;TicketID=100319

Da der kun er tale om 5 årgange, gør vi det manuelt. 
Det her er et godt start-sted http://labs.statsbiblioteket.dk/labsapi/api//api-docs?url=/labsapi/api/openapi.yaml

Vi vil gerne opdatere med et år ved hvert nytår. For at bruge den åbne labs api kræver det at der åbnes op for et 
nyt år i apien. Det kan Toke gøre. Det tager ikke særlig lang tid for Toke. Når det er åbent tager det heller ikke 
ret lang tid for Bolette at udtrække et nyt år og lægge det på LOAR.

## 2025

Filerne er for store til at uploade på web-siden, derfor følgende:

Eksporter sidste års item (uden bitstream)
`/data/dspace/bin/dspace export -t ITEM -i 1902/8769 -d /data/dspace/temp/newspapers -n 3 -x`

Kopier 
`scp -r dspace@dspace-prod-01.kb.dk:/data/dspace/temp/newspapers/3 resources/`

Rediger og kopier ny Simple Archive Format (SAF)
`baj@baj-fedora:~/Projects/meloar-transform/newspapers/src/main$ scp -r resources/4 dspace@dspace-prod-01.kb.dk:/data/dspace/temp/newspapers/`

Importer
`/data/dspace/bin/dspace import -a -e dm-admin@kb.dk -c 1902/283 -s items-dir -m mapfile4`

