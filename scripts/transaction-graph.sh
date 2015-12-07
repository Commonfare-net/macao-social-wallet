#!/usr/bin/env zsh

dbname=freecoin

txeval() {
    mongo $dbname -eval 'db.transactions.find().forEach(printjson)' \
        | sed -e 's/,//g ; s/NumberLong//g ; s/(//g ; s/)//g' \
        | awk '

/amount" :/ { amount=$3 }
/to-id" :/ { recipient=$3 }
/from-id" :/ { sender=$3 }
/^}/ { print "txarr+=(" sender ":" recipient ":" amount ")" }
'}

typeset -a txarr
eval `txeval`

typeset -aU participants
for i in ${txarr}; do
    participants+=( ${i[(ws@:@)1]} )
    participants+=( ${i[(ws@:@)2]} )
done

print "${#txarr} transactions parsed"
print "${#participants} unique participants"

graphviz=`cat << EOF
digraph $dbname {\n
graph [ splines=compound, overlap=false, overlap_shrink=true, ranksep=3, pack=true, packmode=nodes, resolution=120 ];\n
node [shape=ellipse, style=filled];\n
EOF`

for p in ${participants}; do
graphviz+="$p\n"
done

for t in $txarr; do
    sender=${t[(ws@:@)1]}
    recipient=${t[(ws@:@)2]}
    amount=${t[(ws@:@)3]}
    graphviz+="$sender -> $recipient [weight=$amount]\n"
done
graphviz+="}\n"

print $graphviz > $dbname.dot
cat $dbname.dot | twopi -Tpng -o $dbname.png
