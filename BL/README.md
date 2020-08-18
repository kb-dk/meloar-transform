# Ingest BL material

##Some interesting questions

### Files

Lige nu er de filer der kommer med zip-filer. Er det særligt brugbart i LOAR? Hvad med MeLOAR?

### Multiple scannings of same book

Vi har fx multiple skanninger af den samme bog. Her vil al metadata altså være præcis det samme, men filerne vil være forskellige. De kommer lige nu ind som separate poster. fx BL record ID 014824985
- https://dspace-devel.statsbiblioteket.dk/handle/1902/261
- https://dspace-devel.statsbiblioteket.dk/handle/1902/266
- https://dspace-devel.statsbiblioteket.dk/handle/1902/268

###Multiple editions of same book

Vi har også multiple udgaver af samme bog... fx BL record ID 014880504 og 014885805
- https://dspace-devel.statsbiblioteket.dk/handle/1902/262
- https://dspace-devel.statsbiblioteket.dk/handle/1902/269

### Where does the metadata go?

Og så er der noget metadata, jeg ikke rigtig ved hvor jeg skal gøre af:
- Series title (kolonne 16) -> relation.ispartofseries
- Number within series (kolonne 17) -> ???

- Edition (kolonne 24) -> ?

- BL Shelfmark (kolonne 27) -> ?

Ja ok, og så er der nogle stykker jeg har ignoreret. Jeg må hellere skrive en pæn map ned!


