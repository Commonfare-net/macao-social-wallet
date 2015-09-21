#!/usr/bin/env zsh

ext=png

formats=(dot neato twopi circo)

files=`find . -name "*.dot"`

for i in ${(f)files}; do
    for f in ${formats}; do
        print "rendering $i with $f"
        $f -T$ext $i > $i-$f.png
    done
done

