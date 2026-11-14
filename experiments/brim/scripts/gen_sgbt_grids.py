#!/usr/bin/env python3
"""Writes the tuning grids of the base learners inside SGBT (StreamingGradientBoostedTrees) for classification,
configs/sgbt_tuning/sgbt_<base>.tsv: the grids of the single trees (configs/tuning/fimtdd.tsv and brim.tsv) inside SGBT
with its default structure (100 boosting steps, learning rate 0.0125, 75% of the attributes per step), as
gen_ensemble_grids.py does for SGBR. SGBT passes nominal attributes to BRIM as they are (-H, no one-hot encoding);
FIMT-DD cannot split on nominal attributes, so SGBT one-hot encodes them for it."""
import os

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE = "meta.StreamingGradientBoostedTrees{flags} -l ({cli})"


def main():
    for base in ["fimtdd", "brim"]:
        rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(EXP_DIR, "configs", "tuning", f"{base}.tsv"))
                if l.strip() and not l.startswith("#")]
        no_one_hot = base == "brim"
        path = os.path.join(EXP_DIR, "configs", "sgbt_tuning", f"sgbt_{base}.tsv")
        with open(path, "w") as f:
            f.write("# name<TAB>MOA learner CLI (tuning grid of the base learner inside SGBT: the grid of the single "
                    f"trees, configs/tuning/{base}.tsv"
                    + ("; SGBT passes nominal attributes to BRIM without one-hot encoding, -H" if no_one_hot else "")
                    + "; written by scripts/gen_sgbt_grids.py)\n")
            for name, cli in rows:
                f.write(f"SGBT-{name}\t{TEMPLATE.format(cli=cli, flags=' -H' if no_one_hot else '')}\n")
        print(f"wrote {path} ({len(rows)} configurations)")


if __name__ == "__main__":
    main()
