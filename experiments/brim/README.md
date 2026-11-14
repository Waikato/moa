# BRIM experiments

Experiments of the paper *BRIM: Memory-Budgeted, Fast and Plastic Model Trees for Data Stream Regression*
(`paper/brim_paper.tex`). BRIM (Budgeted Restructuring Incremental Model tree) is implemented in MOA as
`moa.classifiers.trees.BRIMRegressor` (module `moa/` of this repository, classes in
`moa/src/main/java/moa/classifiers/trees/BRIMRegressor.java` and `.../trees/brim_util/`). BRIM grew out of the initial
attempt to adapt the PLASTIC classifier to regression (variance reduction and E-BST observers), described in Section 3 of
the paper; `brim_util/` started as a copy of PLASTIC's `plastic_util/`. The results in `results/raw/` were produced
before the class was renamed from `PLASTICRegressor`.

## Layout

| Path | Content |
|---|---|
| `src/PrequentialBenchmark.java` | Prequential runner: accuracy, learner wall time and memory (SizeOf agent) of one run |
| `src/TraceBenchmark.java` | Traces memory and cumulative wall time along the stream (motivating experiment) |
| `scripts/env.sh` | Paths and JVM options (override with environment variables) |
| `scripts/build.sh` | Compiles MOA and the runners, writes `build/cp.txt` |
| `scripts/deploy.sh` | Copies MOA, its dependencies, the runners, scripts, configs and the given datasets to a server |
| `scripts/run.sh` | Runs all (dataset, method, seed) combinations sequentially, resumable |
| `scripts/run_main.sh`, `scripts/run_ablation.sh` | Main experiments and ablation study |
| `scripts/run_tuning.sh`, `scripts/tune_job.sh` | Hyperparameter tuning of BRIM and the baselines on the 10 streams of `configs/tuning_datasets.txt` (the ablation streams without hyperA) |
| `scripts/run_all_single.sh` | Tuning, selection and main experiments in one unattended, resumable run (memory measured at the end of each stream) |
| `configs/tuning/*.tsv` | Tuning grids of BRIM (2,187 configurations), FIMT-DD (27), ORTO (18), AMRules (18) and SGT (18) |
| `analysis/select_baselines.py` | Selects the best configuration of BRIM and each baseline (average rank of adjusted R²), writes `configs/main_methods.tsv` and `configs/ablation_variants.tsv` |
| `analysis/tuning_tables.py` | Per tuning stream: adjusted R² and wall time, with ranks, of the most accurate, the fastest and the selected configuration of each method (`paper/tables/tuning_{best_r2,best_time,selected}.tex`, `results/summary/tuning_*.csv`) |
| `scripts/run_ensemble_tuning.sh`, `scripts/ensemble_tune_job.sh` | Tuning of the base learners (FIMT-DD, BRIM) inside OzaBag and SGBR on the 10 streams of `configs/ensemble_tuning_datasets.txt` |
| `configs/ensemble_tuning/*.tsv` | Ensemble tuning grids: OzaBag and SGBR × FIMT-DD (27 configurations) and BRIM (18) |
| `analysis/select_ensembles.py` | Selects the base learner of each ensemble, writes `configs/ensemble_methods.tsv` |
| `scripts/run_ensemble.sh` | Ensemble experiments (BRIM as a base learner) on the 16 streams |
| `src/PrequentialClassificationBenchmark.java` | Classification counterpart of the prequential runner (SGBT experiment, see below) |
| `scripts/*sgbt*.sh`, `configs/sgbt_*`, `analysis/select_sgbt.py`, `analysis/analyze_sgbt.py` | BRIM as the base learner of SGBT for classification (see [SGBT classification experiment](#sgbt-classification-experiment)) |
| `scripts/run_trace.sh` | Motivating experiment (initial attempt vs. QO vs. BRIM vs. FIMT-DD on FriedmanGra) |
| `configs/main_methods.tsv`, `configs/ablation_variants.tsv` | Methods: `name<TAB>MOA learner CLI` |
| `configs/main_datasets.txt`, `configs/tuning_datasets.txt`, `configs/ablation_datasets.txt` | Datasets (ARFF names without extension) |
| `analysis/analyze.py` | Ranks, Friedman and Shaffer tests, CD diagrams, LaTeX tables |
| `analysis/plot_trace.py` | Figure of the motivating experiment (`paper/figures/trace.pdf`) |
| `results/raw/*.csv` | One line per run (see below) |
| `results/summary/*.csv` | Means and standard deviations over seeds, ranks, average ranks, Shaffer adjusted p-values, tuning rankings |
| `paper/` | The paper's Overleaf project (a git submodule): `brim_paper.tex`, generated figures (`figures/`) and tables (`tables/`) |

## Requirements

- JDK 17 or newer (the results were produced with OpenJDK 26.0.1) and Maven (the MOA dependencies must be in the local
  Maven repository, as the build runs offline: `mvn -o`).
- The SizeOf agent `sizeofag-1.1.0.jar` (also in `~/.m2/repository/com/github/fracpete/sizeofag/1.1.0/`).
  On recent JDKs the agent needs the `--add-opens` options of `scripts/env.sh`; without them it silently undercounts.
- Python 3 with numpy, scipy, pandas and matplotlib.
- pdflatex and bibtex for the paper.
- The datasets: the 16 ARFF files of the `RDatasets` folder (see `configs/main_datasets.txt`), described in
  Gunasekara et al., *Gradient boosted bagging for evolving data stream regression*, Data Mining and Knowledge Discovery
  39:65 (2025), https://doi.org/10.1007/s10618-025-01147-x.

## Reproducing

```bash
cd experiments/brim
export DATA_DIR=/path/to/RDatasets               # default: experiments/brim/RDatasets
export SIZEOF_AGENT=/path/to/sizeofag-1.1.0.jar   # default: ~/Desktop/CODE/sizeofag-1.1.0.jar
scripts/build.sh
scripts/run_tuning.sh      # tuning of BRIM and the baselines on the 10 tuning streams -> results/raw/tuning/
python3 analysis/select_baselines.py   # writes configs/main_methods.tsv with the selected baseline configurations
scripts/run_main.sh        # 16 datasets x 7 methods x 3 seeds -> results/raw/main.csv
scripts/run_ablation.sh    # 11 datasets x 11 variants x 3 seeds -> results/raw/ablation.csv
scripts/run_trace.sh       # motivating experiment -> results/raw/trace.csv (about 3 minutes)
scripts/run_ensemble_tuning.sh         # base learners inside OzaBag and SGBR -> results/raw/ensemble_tuning/ (see below)
python3 analysis/select_ensembles.py   # writes configs/ensemble_methods.tsv
scripts/run_ensemble.sh    # 16 datasets x 6 ensembles x 3 seeds -> results/raw/ensemble.csv (see below)
python3 analysis/analyze.py
python3 analysis/plot_trace.py
cd paper && pdflatex brim_paper && bibtex brim_paper && pdflatex brim_paper && pdflatex brim_paper
# brim_paper.tex has the tables inlined (no \input): copy regenerated tables from paper/tables/ into it
```

`run.sh` skips the runs that are already in the output file, so an interrupted experiment can be resumed with the same
command. Delete the CSV file to start from scratch. A single run can be reproduced with, e.g.,

```bash
source scripts/env.sh
java "${JVM_OPTS[@]}" -cp "$(classpath)" PrequentialBenchmark "$DATA_DIR/hyperA.arff" hyperA BRIM \
     "trees.BRIMRegressor -m 1000000" 1 20
```

## Measures (one CSV line per run)

| Column | Meaning |
|---|---|
| `dataset, config, seed, instances` | Run identification; the seed is set with `setRandomSeed` (it initializes the perceptrons) |
| `mae, rmse, r2, adj_r2` | Final prequential measures of MOA's `BasicRegressionPerformanceEvaluator` |
| `learner_wall_s` | Wall time spent in `getVotesForInstance` and `trainOnInstance` only (no reading or evaluation) |
| `mem_peak_bytes, mem_mean_bytes, mem_final_bytes` | Size of the learner (SizeOf agent): largest and mean over the `MEMORY_SAMPLES` measurements, and at the end (all three equal with the default of one measurement, at the end) |
| `est_peak_bytes, est_final_bytes` | BRIM's own structural size estimate (−1 for other learners) |

The analysis uses the mean over the seeds of `adj_r2` (higher is better), the memory (`mem_final_bytes` for the main
experiments and `mem_peak_bytes` for the ablation) and `learner_wall_s` (lower is
better), ranks the methods on each dataset, and applies the Friedman test (Iman-Davenport) and Shaffer's static post-hoc
procedure for all pairwise comparisons (α = 0.05). CD diagrams link the methods that are not significantly different.

Wall times depend on the machine and its load; each run uses its own JVM and the runs of an experiment are sequential.
The results in `results/raw/` were produced on an Apple M5 Pro (15 cores, 24 GB RAM, macOS), 29 Oct to 1 Nov 2026: the
tuning with 16 parallel jobs, then the main experiments and the ablation one run at a time, at different times.

## Methods

Main experiments (`configs/main_methods.tsv`): FIMT-DD, ORTO, AMRules, SGT (StochasticGradientTree) and BRIM with the
configurations selected by tuning; BRIM-fast, the BRIM configuration with the best average rank in learner wall time
in the tuning (ties: higher median adjusted R²; `-g 500 -c 1e-7 -R 2000 -k 2 -q 0.5 -D 20 -B 5 -V`); and both BRIMs with
budgets of 1 MB and 100 KB (`-m`; tuned with the default 32 MB budget).

Tuning: each configuration of a grid (`configs/tuning/`) is run on the 10 tuning streams (`configs/tuning_datasets.txt`: the ablation streams without hyperA, the longest, which stays in the main experiments) with seeds 1–3; the
configurations are ranked on each stream by their mean adjusted R² and the best average rank is selected. A run is
stopped after 10 minutes (`TUNING_TIME_LIMIT`; runs take seconds on these streams) and, like a diverged run, ranks last
on its stream. `scripts/run_tuning.sh` splits the BRIM grid into `BRIM_PARTS` (12) jobs per
stream. The selection also writes `configs/ablation_variants.tsv` from the selected BRIM.

| Method | Grid | Selected |
|---|---|---|
| BRIM | `-g` {50, 200, 500} × `-c` {1e-2, 1e-4, 1e-7} × re-evaluation period `-R` {200, 2000, 20000} × attributes per inner node `-k` {2, 3, 5} × quantization radius `-q` {0.125, 0.25, 0.5} × maximum depth `-D` {5, 10, 20} × maximum branch length `-B` {2, 3, 5} (2,187 configurations, all with re-evaluation after a drift, `-V`; adaptive leaves, drift detection and the 32 MB budget at their defaults) | `trees.BRIMRegressor -g 500 -c 0.01 -R 200 -k 5 -q 0.125 -D 20 -B 3 -V` |
| FIMT-DD | `-g` {50, 200, 500} × `-c` {1e-2, 1e-4, 1e-7} × leaves {perceptron `-l 0.02`, perceptron `-l 0.002`, mean `-e`} | `trees.FIMTDD -g 200 -c 0.01 -e` |
| ORTO | `-g` {50, 200, 500} × the same leaves × `-o` {average, bestTree} | `trees.ORTO -g 500 -e -o average` |
| AMRules | `-g` {50, 200, 500} × `-L` {Adaptive, Mean, Perceptron}MultiTargetRegressor × ordered/unordered (`-U`) | `rules.AMRulesRegressor -g 50` |
| SGT | `-G` {50, 200, 500} × `-C` {1e-2, 1e-4, 1e-6} × warm start `-W` {100, 1000} | `trees.StochasticGradientTree -G 50 -C 0.01 -W 100` |

The results of the baselines with MOA's default parameters are kept in `results/raw/main_default_baselines.csv`.

Ensembles (`configs/ensemble_methods.tsv`), BRIM as a base learner instead of FIMT-DD: OzaBag with 10 members and SGBR
(`meta.StreamingGradientBoostedRegression`: 10 boosting steps, each an OzaBag with 10 members, learning rate 1), the
structure of SGBR's defaults. Only the base learner is tuned, inside each ensemble, on the ablation streams except hyperA
(`configs/ensemble_tuning_datasets.txt`; hyperA is the most expensive with SGBR) with seeds
1–3: FIMT-DD over the grid of the single-tree tuning, BRIM over `-g` {50, 200, 500} × `-c` {1e-2, 1e-4, 1e-7} × leaves
{adaptive, mean `-e`}. The `-1MB` variants limit each BRIM member to 1 MB (`-m 1000000`).

The ensemble experiments are much more expensive than the others (SGBR trains 100 trees) and run on a server
(`scripts/build.sh`, then `scripts/deploy.sh <user@host> <dir> RDatasets`), with the settings of all experiments
(see [Run settings](#run-settings)):

- Tuning (2,700 runs): `PARALLEL=8 scripts/run_ensemble_tuning.sh`; a run is stopped after `TUNING_TIME_LIMIT` (20 minutes)
  and ranks last on its dataset. `GRIDS` and `SEEDS` select a subset, e.g., to split the tuning across machines
  (`GRIDS="sgbr_brim" SEEDS="1"`).
- Final runs (288 runs): `scripts/run_ensemble.sh` runs them sequentially, as the main experiments, so that wall times
  are comparable; `PARALLEL=n` runs n jobs (one per dataset and seed, largest datasets first) and merges their files
  (`results/raw/ensemble_parts/`) into `results/raw/ensemble.csv`. A run is stopped after `FINAL_TIME_LIMIT` (3 hours).

`scripts/run_all_ensembles.sh` runs tuning, selection and the ensembles unattended (e.g., in tmux), with
`TUNING_PARALLEL` and `MAIN_PARALLEL` jobs and `PYTHON` for the selection. Then `python3 analysis/analyze.py --only
ensemble` writes `paper/tables/ensemble_*.tex` and `paper/figures/cd_ensemble_*`.

Ablation (`configs/ablation_variants.tsv`), the selected BRIM with one component removed (an option of the variant
replaces the tuned one, e.g., `-k 0`):

| Variant | Option | Component removed |
|---|---|---|
| no-QO | `-O` | Quantization observer (E-BST of FIMT-DD instead) |
| no-adaptive-leaf | `-N` | Adaptive leaves (always the perceptron) |
| mean-leaf | `-e` | Perceptrons (always the mean target) |
| no-clipping | `-C` | Clipping of the perceptron inputs and errors |
| no-drift-detection | `-u` | Drift detection and background trees |
| no-guarded-replacement | `-I` | Guarded replacement (replace at once on a drift, as ARF) |
| no-attribute-subset | `-k 0` | Attribute subsetting at inner nodes (monitor all attributes) |
| no-restructuring | `-B 1` | Restructuring (EFDT-style: replace the subtree) |
| no-reevaluation | `-R 2000000000` | Re-evaluation of inner nodes (Hoeffding-tree style) |
| multiway-nominal | `-M` | Binary splits on nominal attributes (multiway instead) |

## BRIM options

`trees.BRIMRegressor` options (defaults in brackets): `-g` grace period [200], `-R` re-evaluation period [200],
`-c` split confidence [1e-7], `-t`/`-T` tie thresholds [0.05], `-G` relative minimum merit difference [0.5], `-D` maximum
depth [20], `-B` maximum branch length for restructuring [5], `-q` QO radius relative to the attribute's standard deviation
[0.25], `-k` attributes monitored by inner nodes [5], `-m` memory budget in bytes [33554432, 0 = none], `-E` memory check
period [100], `-u` disable drift detection, `-V` re-evaluate the inner nodes' splits after a drift, `-w`/`-x` warning/drift detectors [ADWIN 1e-2/1e-3], `-F` fraction of the budget
for the background tree [0.5], `-f` fading factor [0.995], `-N` no adaptive leaves, `-e` mean leaves, `-M` multiway
nominal splits, `-l`/`-d`/`-p` perceptron learning rate, decay and constant rate, `-Z` random seed, and the ablation
switches `-O`, `-C`, `-I`.

## Run settings

All experiments use the same JVM settings (`scripts/env.sh`):

- Maximum heap `HEAP=30g`: enough for the largest ensembles and for measuring them with the SizeOf agent, which needs
  about as much heap again as the model, and below 32 GB, so that the JVM keeps compressed object pointers and model
  sizes stay comparable. A server must have room for the jobs that actually grow large: four 30 GB JVMs plus their
  overhead exceeded the 125 GB of marseille, and the Linux OOM killer stopped runs.
- Memory is measured once, at the end of the stream (`MEMORY_SAMPLES=1`; `mem_final_bytes` is the final model size).
  Only the ablation (`scripts/run_ablation.sh`) keeps the peak over 20 evenly spaced points, as reported in the paper.
- `TIME_LIMIT` stops a run with GNU `timeout` (0 = none, the default); the tuning scripts of the ensembles use 20 minutes
  and their final runs 3 hours (`TUNING_TIME_LIMIT`, `FINAL_TIME_LIMIT`). A stopped run is recorded with its dataset,
  method and seed and empty measures (`instances` empty, unlike a diverged run), so it is not retried on resume and a
  selection script ranks it last; its log is `results/logs/<dataset>.<method>.<seed>.timeout`. A failed run (no result
  line) is not recorded; its log `.err` ends with the JVM's exit code (above 128 for a death by signal, e.g., 137 =
  SIGKILL from the OOM killer).
- When the SizeOf agent cannot measure a large model at once (out of memory, or `IllegalStateException: Capacity
  exhausted` when its identity map of visited objects reaches about 2^29 objects), both runners measure it in parts:
  the full sizes of the sub-learners plus the shallow sizes of the objects outside them (within 0.01% of the full
  measurement on a smaller SGBR-FIMTDD; objects shared by the trees, e.g., the header, are counted once per tree, a few
  KB each). The runner notes it on stderr, kept as `results/logs/<dataset>.<method>.<seed>.notes`.

## Results at a glance

`results/raw/main.csv` (480 runs: 10 methods × 16 datasets × seeds 1–3), `results/raw/ablation.csv` (396 runs: 12
variants × 11 datasets × 3 seeds) and `results/raw/tuning/` (68,040 runs, none stopped at the time limit: 65,610 of
BRIM's grid, 810 of FIMT-DD's and 540 each of ORTO's, AMRules' and SGT's), all with the [run settings](#run-settings)
above; summaries in `results/summary/` (`python3 analysis/analyze.py`). The memory is the final model size in the main
experiments and the peak over 20 measurements in the ablation. In total, the learners trained for 58 hours in the
tuning, 44 minutes in the main experiments and 9 minutes in the ablation.

| Measure (average rank, 16 datasets) | FIMTDD | ORTO | AMRules | SGT | BRIM | BRIM-1MB | BRIM-100KB | BRIM-fast | BRIM-fast-1MB | BRIM-fast-100KB |
|---|---|---|---|---|---|---|---|---|---|---|
| Adjusted R² (higher is better) | 7.50 | 9.25 | 6.56 | 7.88 | **2.53** | 2.84 | 5.41 | 4.31 | 4.25 | 4.47 |
| Final memory | 8.56 | 7.38 | 7.25 | 9.25 | 6.28 | 4.91 | 1.84 | 4.53 | 3.59 | **1.41** |
| Learner wall time | 3.31 | 3.69 | 9.06 | 9.38 | 7.25 | 6.62 | 3.19 | 5.06 | 4.69 | **2.75** |

The Friedman test rejects equal ranks for all three measures (p < 10⁻¹⁰). Shaffer's post-hoc test (α = 0.05):

- Adjusted R²: BRIM and BRIM-1MB are significantly more accurate than all four baselines; BRIM-fast, BRIM-fast-1MB
  and BRIM-fast-100KB than ORTO and SGT; BRIM-100KB than ORTO. No two BRIM variants differ significantly (BRIM vs.
  BRIM-100KB: p = 0.17).
- Final memory: BRIM-100KB and BRIM-fast-100KB are significantly smaller than all baselines and than BRIM.
- Wall time: FIMT-DD is significantly faster than BRIM and BRIM-1MB, and ORTO than BRIM; BRIM-100KB and BRIM-fast-100KB
  are significantly faster than BRIM, AMRules and SGT.

Geometric means over the datasets of the final memory: BRIM 569 KB (9.6% of FIMT-DD's), BRIM-1MB 293 KB (4.9%),
BRIM-100KB 43 KB (0.7%), BRIM-fast 320 KB (5.4%), BRIM-fast-1MB 167 KB (2.8%), BRIM-fast-100KB 43 KB (0.7%), FIMT-DD
5.9 MB, ORTO 3.3 MB, AMRules 1.4 MB, SGT 27.4 MB; of the learner wall time: BRIM 1.61 s (2.5 times FIMT-DD's), BRIM-1MB
1.49 s, BRIM-100KB 0.85 s (1.3 times), BRIM-fast 1.17 s (1.8 times), BRIM-fast-100KB 0.82 s (1.3 times), FIMT-DD 0.64 s,
ORTO 0.68 s, AMRules 3.09 s, SGT 4.81 s (tuned FIMT-DD uses mean leaves). At the end of every main run, both BRIM's
structural size estimate and the measured model size are within the budget (the main runs measure both only at the
end).

Ablation (average ranks over 11 datasets; Friedman p < 10⁻⁶ for all three measures): the selected BRIM has the best
average rank in adjusted R² (3.95; multiway-nominal 4.05, no-attribute-subset 4.91, no-reevaluation 5.18, no-QO 5.73,
no-drift-reevaluation 5.95), and only mean-leaf is significantly less accurate (11.45). In peak memory, no-QO is
significantly larger (11.91) and no-restructuring significantly smaller (1.27) than BRIM (6.59); in wall time,
no-restructuring (2.36), mean-leaf (3.09) and no-reevaluation (3.27) are significantly faster than BRIM (8.55).

## SGBT classification experiment

A separate experiment validates BRIM as the base learner of Streaming Gradient Boosted Trees
(`meta.StreamingGradientBoostedTrees`, SGBT) for data stream **classification**, replacing its default base learner
FIMT-DD. SGBT keeps its default structure: 100 boosting steps, learning rate 0.0125, 75% of the attributes per step,
softmax cross-entropy loss, and one booster per class (one-vs-rest) for more than two classes, i.e., 100 trees for
binary streams and 100 × (number of classes) trees otherwise. Each tree regresses the pseudo-residual
(gradient / hessian) with weight 1.

Two changes to `StreamingGradientBoostedTrees` (module `moa/`) were made for this experiment:

- The `System.gc()` call every 1000 training instances (per booster) was removed. A forced full GC traces all live
  objects, so its cost grows with the model: on LED (10 boosters), FIMT-DD paid 100–200 ms per call (about 1,000 calls
  per 100K instances) and BRIM about 12 ms, all counted as learner time.
- With `-H` (disable one-hot encoding), the warning about FIMT-DD not supporting nominal attributes checked the default
  base learner instead of the configured one, so it was printed for every base learner.

Base learners: FIMT-DD gets nominal attributes one-hot encoded by SGBT (it does not support nominal attributes); its
tuned configuration (`-g 50 -c 0.01 -e`) differs from SGBT's default (`-g 25 -c 0.05 -e -p`), which runs as `SGBT-default`.
All 18 FIMT-DD configurations with perceptron leaves diverged on every tuning stream. BRIM gets the nominal attributes as they are (`-H`;
on RTG with eager splits, 83.5% accuracy instead of 75.7% with one-hot encoding), with drift detection and its default
32 MB budget per tree. BRIM is tuned with its adaptive leaves; the final runs add mean leaves (`-e`) to every BRIM
configuration, since a timing test (LED_a_100K, RBF5_f_100K and RTG_50K) showed them 22–24% faster at the same or better accuracy (73.15 vs. 73.09, 68.54 vs. 67.75, 83.53 vs. 83.57),
whereas disabling drift detection (`-u`) saved only 4–11% and cost up to 1 point. `SGBT-BRIM-1MB`, `-500KB` and `-100KB`
limit each tree to 1 MB, 500 KB and 100 KB (`-m 1000000`, `500000`, `100000`); the budget is not tuned. A budget test
on the same streams: 100 KB costs nothing on LED_a (73.08 vs. 73.09) but 6–7 points on RTG_50K
and RBF5_f_100K, 1 MB at most 0.9 points. Two fast BRIM configurations run next to the best one: `SGBT-BRIM-fastgood`,
the fastest of the best 10% by average rank (`-g 50 -c 0.01 -R 200 -k 2 -q 0.125 -D 5 -B 2 -V`; 1,493 s in total on the
tuning streams vs. 2,506 s of the best, 3 points less mean accuracy), and `SGBT-BRIM-fast`, the fastest of all
(`-g 500 -c 1e-7 -R 200 -k 2 -q 0.5 -D 10 -B 2 -V`; 1,196 s, 8 points less).

| Step | Script | Content |
|---|---|---|
| Datasets | `scripts/gen_cdatasets.sh` | Synthetic streams in `CDatasets/` (1M or 100K instances) and the 20K-instance tuning streams in `CDatasets_tuning/` (`SCALE=0.02` for the 1M streams, 0.2 for the 100K streams), plus elecNormNew, airlines and covtypeNorm in `CDatasets/` |
| Build | `scripts/build.sh` | MOA and the runners (compiled for Java 21, the servers' version) |
| Deploy | `scripts/deploy.sh <user@host> <dir> CDatasets CDatasets_tuning` | Copies the MOA classes, the dependency jars and the SizeOf agent (`lib/`), the runners, scripts, configs and data to a server without Maven, and writes a `build/cp.txt` with the server paths |
| Grids | `scripts/gen_sgbt_grids.py` | Writes `configs/sgbt_tuning/sgbt_{fimtdd,brim}.tsv`: the single-tree grids (`configs/tuning/`: FIMT-DD 27 configurations, BRIM 2,187) inside SGBT with its default structure (100 boosting steps), as `gen_ensemble_grids.py` does for SGBR; BRIM gets nominal attributes as they are (`-H`), FIMT-DD one-hot encoded |
| Tuning | `scripts/run_sgbt_tuning.sh` | The 2,214 configurations of `configs/sgbt_tuning/`, on the 7 streams of `configs/sgbt_tuning_datasets.txt` (`CDatasets_tuning/`, 20K instances each), seed 1, at most 1 hour per run -> `results/raw/sgbt_tuning/parts/` (not in git) and their merge `results/raw/sgbt_tuning.csv`, from which `select_sgbt.py` selects when the parts are absent |
| Selection | `analysis/select_sgbt.py` | Best average rank of accuracy; diverged or failed runs rank last. Writes `configs/sgbt_methods.tsv` (tuned SGBT-FIMTDD, tuned SGBT-BRIM, SGBT-BRIM-1MB, -500KB and -100KB, `SGBT-BRIM-fastgood` and `SGBT-BRIM-fast` (the fastest BRIM configurations of the best 10% and of all, by total wall time), and `SGBT-default`, SGBT with its default FIMT-DD, unless tuning selects that configuration: then the runs would be identical and `SGBT-default` is left out) and `paper/tables/sgbt_tuning.tex` |
| Final runs | `scripts/run_sgbt_final.sh` | `configs/sgbt_methods.tsv` on all 14 streams of `CDatasets/` (`configs/sgbt_datasets.txt`), seeds 1–3, at most 10 hours per run (`FINAL_TIME_LIMIT`; a 100K-instance test of the best BRIM on LED_g_1M extrapolates to about 3 hours alone and up to about 6.5 hours with 8 jobs on LED_a_1M and LED_g_1M) -> `results/raw/sgbt.csv` |
| Analysis | `analysis/analyze_sgbt.py` | Accuracy and wall time of the selected configurations on the tuning streams (`paper/tables/sgbt_tuning_streams.tex`); accuracy, final memory and wall time of the final runs per stream, mean ± sd over the completed seeds, with the runs stopped at the time limit (TO) or failed (OOM: out of heap; killed: by a signal) (`results/summary/sgbt_final*.csv`, `paper/tables/sgbt_final.tex`); average ranks (a method without a completed seed on a stream ranks last there), Friedman and Shaffer tests and CD diagrams (`paper/tables/sgbt_avg_ranks.tex`, `paper/figures/cd_sgbt_*`) |

`scripts/run_all_sgbt.sh` runs tuning, selection and final runs unattended, e.g., on marseille.hh.se (20 cores, 125 GB,
Java 21), with `TUNING_PARALLEL`, `FINAL_PARALLEL` and `PYTHON` (a Python 3 with pandas and scipy). It does not make
the selection while tuning runs are missing (failed): rerun it to retry them, or set `ALLOW_MISSING=1` to rank them last.

```bash
PYTHON=/data2/home/nuwgun/conda_envs/CondaGR/bin/python \
  scripts/run_all_sgbt.sh 2>&1 | tee -a results/logs/run_all_sgbt.log
```

`scripts/run_sgbt.sh` is the classification counterpart of `run.sh` (sequential, resumable) and
`scripts/run_sgbt_parallel.sh` runs one job per (dataset, method, seed), the most expensive streams (size × classes) first.
Each run uses one core: SGBT's parallel streams are disabled (`FJ_PARALLELISM`, default 0) and the GC threads of each
JVM limited (`GC_THREADS`, default 2), so that parallel jobs do not overload the server; wall times are still affected by
the other jobs. FIMT-DD needs several GB on the multi-class streams: with 12 tuning jobs, six FIMT-DD jobs on
RBF5_f_100K filled marseille's 125 GB within 10 minutes. With the 30 GB heap, G1 commits far more than the live model:
within 4 minutes on RBF5_f_100K, FIMT-DD JVMs reached 25 GB and BRIM JVMs 6–10 GB (capping the young generation,
`-XX:MaxNewSize`, did not help), so memory, not cores, limits the parallelism. The scheduler of `run_sgbt_parallel.sh`
runs at most `PARALLEL` jobs, at most `LARGE_PARALLEL` of them of the methods matching `LARGE_METHODS` (those whose
models can fill the heap), starts a job only while `MIN_FREE_GB` GB are available and `START_GAP` seconds after the
previous start (a JVM grows mostly in its first minutes), and, if the available memory falls below `EMERGENCY_FREE_GB`,
stops the most recently started job, queues it again and lowers the number of parallel jobs, which it raises again by
one every `RECOVER_MIN` minutes (default 30) without a stop. On a restart, the jobs already recorded are skipped at
once. `run_all_sgbt.sh` gives the tuning and the final runs their own settings (`TUNING_*`, `FINAL_*`), with 6 GB for
the emergency stop in both:

| Setting | Tuning (15,498 runs on 20K instances) | Final runs (336 runs on up to 1M instances) |
|---|---|---|
| Jobs at a time (`*_PARALLEL`) | 16 | 8 |
| Heap per job (`*_HEAP`) | 8 GB | 30 GB |
| Memory available to start a job (`*_MIN_FREE_GB`) | 10 GB | 32 GB (16 GB for the budgeted BRIM variants, `SMALL_MIN_FREE_GB`) |
| Seconds between starts (`*_START_GAP`) | 15 | 240 |
| Jobs at a time of the methods that can fill the heap (`*_LARGE_PARALLEL`, `*_LARGE_METHODS`) | 3 FIMT-DD | 3 FIMT-DD or unbudgeted BRIM, the fast configurations included (up to 32 MB for each of up to 1,000 trees) |

The tuning streams are five times shorter than the 100K streams on which FIMT-DD reached 9 GB, so an 8 GB heap is
expected to suffice; a run that runs out of heap is not recorded, and the selection waits until it is rerun (or
`ALLOW_MISSING=1`). With the full grids, the tuning is estimated at roughly 1,300 CPU hours (3–4 days on marseille).

The final runs were split across two servers with the same CPU (Xeon W-2255, 20 cores, 125 GB), with the same code,
data and settings. marseille ran covtypeNorm, LED_a_1M, LED_g_1M, RBF2_f_1M, RBF5_f_1M and RBF5_m_1M. The 8 streams
it had not started by 11 Nov (AGR_a_1M, AGR_g_1M, LED_100K, RBF2_m_1M, RBF5_100K, RTG_100K, airlines and elecNormNew,
192 runs) ran on lyon.hh.se (`/data2/home/nuwgun/brim_sgbt_lyon`), 8 jobs at a time like marseille. marseille skipped
them through emptied stream lists in `results/raw/sgbt/lists/`, and lyon's parts replace marseille's header-only parts
of those streams when the results are merged.

SGBT-default (1,000 trees on LED) builds models of hundreds of millions of objects. On them, the end-of-stream memory
measurement first tries the whole model, which runs for hours before the SizeOf agent's set of visited objects fills
and the runner measures the model tree by tree (RBF5_100K: about 8 hours after 22 minutes of learning). On marseille,
SGBT-default on LED_g_1M seed 1 therefore reached the 10-hour limit. All 9 SGBT-default runs on LED_g_1M, LED_a_1M
and RBF2_f_1M (seeds 1–3; on 12 Nov, LED_g_1M seed 2 was stopped after 7.7 hours and the others had not started) ran on
lyon instead, with
`-DmeasureInParts=true`, which measures tree by tree from the start: on RBF5_100K the memory is the same
(15,965,388,344 bytes) and the run takes 24 minutes instead of 8.6 hours. They ran 3 at a time, as marseille's runs of
the large methods on these streams did (`build/start_default_rerun.sh` on lyon, parts in
`results/raw/sgbt_default/parts/`). The learning time is measured without the memory measurement, but it depends on
the load: SGBT-default on RBF5_100K learned in 1,307–1,370 s with 8 jobs at a time and in 1,133 s alone.

All 336 final runs completed (none stopped at the time limit, none with non-finite votes). `results/raw/sgbt.csv`
merges marseille's 135 runs, lyon's 192 and the 9 SGBT-default reruns; the per-server parts are kept in
`results/raw/sgbt_marseille/`, `results/raw/sgbt_lyon/` and `results/raw/sgbt_default_lyon/`.

The classification runner writes `dataset, config, seed, instances, accuracy, kappa, kappa_t, kappa_m` (percent, from
MOA's `BasicClassificationPerformanceEvaluator`), `nonfinite_votes` (instances whose votes contain NaN or infinite
values), `learner_wall_s` and `mem_peak_bytes, mem_mean_bytes, mem_final_bytes` (−1 without memory measurement).
