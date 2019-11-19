#!/usr/bin/env bash
export IFS=","
cat "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/doisandurls.csv" | while read a b ;
do curl -H "Content-Type:text/plain;charset=UTF-8" -X PUT --user DK.SB:*** -d "$(printf 'doi=$a\nurl=https://loar.kb.dk/handle/$b')" https://mds.datacite.org/doi/$a -i;
done