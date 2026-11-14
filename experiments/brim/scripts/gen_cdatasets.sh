#!/bin/bash
# Writes the synthetic classification streams to ARFF files (skips files that already exist). Each file is named
# <stream>_<instances>, e.g. LED_a_1M. SCALE multiplies the number of instances and the drift position (-p) and
# width (-w) of the ConceptDriftStreams.
#   scripts/gen_cdatasets.sh            # all streams, random_seed=1, into CDatasets/
#   SEED=2 scripts/gen_cdatasets.sh LED_a RTG
#   SCALE=0.02 OUT_DIR=CDatasets_tuning scripts/gen_cdatasets.sh AGR_g LED_g RBF5_f RBF5_m   # SGBT tuning streams (20K)
#   SCALE=0.2 OUT_DIR=CDatasets_tuning scripts/gen_cdatasets.sh LED RBF5 RTG
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"
random_seed="${SEED:-1}"
SCALE="${SCALE:-1}"
OUT_DIR="${OUT_DIR:-$EXP_DIR/CDatasets}"
mkdir -p "$OUT_DIR"

# stream<TAB>maxInstances<TAB>stream options
STREAMS="$(cat <<EOF
LED_a	1000000	ConceptDriftStream -s (generators.LEDGeneratorDrift -d 1)   -d (ConceptDriftStream -s (generators.LEDGeneratorDrift -d 3) -d (ConceptDriftStream -s (generators.LEDGeneratorDrift -d 5)  -d (generators.LEDGeneratorDrift -d 7) -w 50 -p 250000 ) -w 50 -p 250000 ) -w 50 -p 250000 -r $random_seed
LED_g	1000000	ConceptDriftStream -s (generators.LEDGeneratorDrift -d 1)   -d (ConceptDriftStream -s (generators.LEDGeneratorDrift -d 3) -d (ConceptDriftStream -s (generators.LEDGeneratorDrift -d 5)  -d (generators.LEDGeneratorDrift -d 7) -w 50000 -p 250000 ) -w 50000 -p 250000 ) -w 50000 -p 250000 -r $random_seed
AGR_a	1000000	ConceptDriftStream -s (generators.AgrawalGenerator -f 1) -d (ConceptDriftStream -s (generators.AgrawalGenerator -f 2) -d (ConceptDriftStream -s (generators.AgrawalGenerator )   -d (generators.AgrawalGenerator -f 4) -w 50 -p 250000 ) -w 50 -p 250000 ) -w 50 -p 250000 -r $random_seed
AGR_g	1000000	ConceptDriftStream -s (generators.AgrawalGenerator -f 1) -d (ConceptDriftStream -s (generators.AgrawalGenerator -f 2) -d (ConceptDriftStream -s (generators.AgrawalGenerator )   -d (generators.AgrawalGenerator -f 4) -w 50000 -p 250000 ) -w 50000 -p 250000 ) -w 50000 -p 250000 -r $random_seed
RBF5_m	1000000	generators.RandomRBFGeneratorDrift -c 5 -s .0001 -r $random_seed -i $random_seed
RBF5_f	1000000	generators.RandomRBFGeneratorDrift -c 5 -s .001 -r $random_seed -i $random_seed
RBF2_m	1000000	generators.RandomRBFGeneratorDrift -c 2 -s .0001 -r $random_seed -i $random_seed
RBF2_f	1000000	generators.RandomRBFGeneratorDrift -c 2 -s .001 -r $random_seed -i $random_seed
RTG	100000	generators.RandomTreeGenerator -r $random_seed -i $random_seed
RBF5	100000	generators.RandomRBFGenerator -r $random_seed -i $random_seed -c 5
LED	100000	generators.LEDGenerator -i $random_seed
EOF
)"

scale_int() { awk -v x="$1" -v s="$SCALE" 'BEGIN { printf "%d", x * s + 0.5 }'; }
# 1000000 -> 1M, 50000 -> 50K
count_label() {
  if (( $1 % 1000000 == 0 )); then echo "$(( $1 / 1000000 ))M"
  elif (( $1 % 1000 == 0 )); then echo "$(( $1 / 1000 ))K"
  else echo "$1"; fi
}

CP="$(classpath)"
while IFS=$'\t' read -r stream_name max stream; do
  if [ $# -gt 0 ] && [[ " $* " != *" $stream_name "* ]]; then continue; fi
  max="$(scale_int "$max")"
  stream="$(SCALE="$SCALE" perl -pe 's/(-[wp]) (\d+)/"$1 " . int($2 * $ENV{SCALE} + 0.5)/ge' <<< "$stream")"
  name="${stream_name}_$(count_label "$max")"
  out="$OUT_DIR/$name.arff"
  if [ -s "$out" ]; then echo "skip $name (exists)"; continue; fi
  echo "write $name ($max instances): $stream"
  "$JAVA" -Xmx"$HEAP" -cp "$CP" moa.DoTask "WriteStreamToARFFFile -s ($stream) -f $out -m $max" > /dev/null \
    || { echo "FAILED $name" >&2; rm -f "$out"; }
done <<< "$STREAMS"
