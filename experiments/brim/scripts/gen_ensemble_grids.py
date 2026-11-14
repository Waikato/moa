#!/usr/bin/env python3
"""Writes the tuning grids of the base learners inside the ensembles, configs/ensemble_tuning/<ensemble>_<base>.tsv:
the grids of the single trees (configs/tuning/fimtdd.tsv and brim.tsv) inside OzaBag (10 members) and SGBR (10 boosting
steps of OzaBag with 10 members). SGBR passes nominal attributes to BRIM as they are (-H, no one-hot encoding); FIMT-DD
cannot split on nominal attributes, so SGBR one-hot encodes them for it."""
import os

EXP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENSEMBLES = {"oza": ("OzaBag", "meta.OzaBag -s 10 -l ({cli})"),
             "sgbr": ("SGBR", "meta.StreamingGradientBoostedRegression{flags} -l (meta.OzaBag -s 10 -l ({cli}))")}


def main():
    for base in ["fimtdd", "brim"]:
        rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(EXP_DIR, "configs", "tuning", f"{base}.tsv"))
                if l.strip() and not l.startswith("#")]
        for key, (label, template) in ENSEMBLES.items():
            no_one_hot = key == "sgbr" and base == "brim"
            path = os.path.join(EXP_DIR, "configs", "ensemble_tuning", f"{key}_{base}.tsv")
            with open(path, "w") as f:
                f.write("# name<TAB>MOA learner CLI (tuning grid of the base learner inside the ensemble: the grid of the "
                        f"single trees, configs/tuning/{base}.tsv"
                        + ("; SGBR passes nominal attributes to BRIM without one-hot encoding, -H" if no_one_hot else "")
                        + "; written by scripts/gen_ensemble_grids.py)\n")
                for name, cli in rows:
                    f.write(f"{label}-{name}\t{template.format(cli=cli, flags=' -H' if no_one_hot else '')}\n")
            print(f"wrote {path} ({len(rows)} configurations)")


if __name__ == "__main__":
    main()
