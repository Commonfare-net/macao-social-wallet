#!/usr/bin/env zsh

dbname=fxctest1

txeval() {
    mongo $dbname -eval 'db.transactions.find().forEach(printjson)' \
        | sed -e 's/,//g' \
        | awk '

/amount" :/ { amount=$3 }
/to" :/ { recipient=$3 }
/from" :/ { sender=$3 }
/^}/ { print "txarr+=(" sender ":" recipient ":" amount ")" }
'}

typeset -a txarr
eval `txeval`

graphviz="# ${#txarr} transactions parsed\n"

graphviz+="digraph $dbname {\n"
for t in $txarr; do
    sender=${t[(ws@:@)1]}
    recipient=${t[(ws@:@)2]}
    amount=${t[(ws@:@)3]}
    graphviz+="$sender -> $recipient [weight=$amount]\n"
done
graphviz+="}\n"

print $graphviz | circo -Tpng -o $dbname.png
