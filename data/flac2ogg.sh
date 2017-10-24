#!/bin/sh

for fn in `ls *.flac`; do
    oggenc -Q --discard-comments -q 2.5 --max-bitrate 192 --advanced-encode-option bitrate_hard_max=192 ${fn}
done

echo "Converted flac to ogg, You may copy to resources/audio/"
