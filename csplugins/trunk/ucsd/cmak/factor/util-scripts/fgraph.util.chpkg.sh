#!/bin/tcsh

echo "package fgraph.util;" > tmp
echo "" >> tmp

foreach file (*.java)
    echo $file
    cat tmp $file > ${file}.tmp
    mv $file ${file}.old
    mv ${file}.tmp $file
end

rm tmp
