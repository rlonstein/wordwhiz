#!/bin/sh -e

DBFN="words.db.sqlite"

sqlite3 ${DBFN} <<EOF
create table dictionary(word varchar UNIQUE);
.mode csv
.import word.list dictionary
EOF

echo "Done. Copy '${DBFN}' to resources/"
