# BRIM experiments

Experiments of the paper *BRIM: Memory-Budgeted, Fast and Plastic Model Trees for Data Stream Regression*
(`paper/main.tex`). BRIM (Budgeted Restructuring Incremental Model tree) is implemented in MOA as
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
| `paper/` | LaTeX sources, generated figures (`figures/`) and tables (`tables/`) |

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
cd paper && pdflatex main && bibtex main && pdflatex main && pdflatex main
python3 flatten.py          # optional: brim_paper.tex, a single-file version with the tables inlined
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
The results in `results/raw/` were produced on an Apple M5 Pro (15 cores, 24 GB RAM, macOS) with the main experiments and the
ablation running as two concurrent sequential streams.

## Methods

Main experiments (`configs/main_methods.tsv`): FIMT-DD, ORTO, AMRules, SGT (StochasticGradientTree) and BRIM with the
configurations selected by tuning, and the selected BRIM with budgets of 1 MB and 100 KB (`-m`; tuned with its default
32 MB budget).

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

The ensemble experiments are much more expensive than the others (SGBR trains 100 trees) and are meant to run on a
server (`scripts/build.sh` first; `env.sh` falls back to the SizeOf agent of the local Maven repository):

- Tuning (2,700 runs): `PARALLEL=8 scripts/run_ensemble_tuning.sh`. Memory is not measured (`MEMORY_SAMPLES=0`), since
  measuring multi-GB SGBR models takes longer than training them. Each job gets `HEAP` (default 10 GB); SGBR with FIMT-DD
  perceptron leaves reaches 2 GB and more. `GRIDS` and `SEEDS` select a subset, e.g., to split the tuning across machines
  (`GRIDS="sgbr_brim" SEEDS="1"`). On an Apple M5 Pro, the SGBR-BRIM runs took 9.3 CPU hours of learner time in total.
- Main runs (288 runs): `scripts/run_ensemble.sh` runs them sequentially, as the main experiments, so that wall times are
  comparable; `PARALLEL=n` runs n jobs (one per dataset and seed, largest datasets first) and merges their files
  (`results/raw/ensemble_parts/`) into `results/raw/ensemble.csv`. Each job gets `HEAP` (default 20 GB: SGBR with FIMT-DD
  ran out of 8 GB on SUP3G). Single runs on an Apple M5 Pro (seed 1): SGBR-BRIM took 54 minutes on NZEnergy and 6 minutes
  on SUP3G, SGBR-FIMTDD 15 minutes on NZEnergy.
- On lyon (`MAIN_PARALLEL=4`, 20 GB per job), the SizeOf agent ran out of heap measuring SGBR-FIMTDD on SUP3A and SUP3G
  (all seeds): its set of visited objects does not fit next to a model of several GB. With 30 GB, the set (an
  `IdentityHashMap`) instead reached its maximum capacity of about 2^29 objects (`IllegalStateException: Capacity
  exhausted`, after 6 to 8 hours per run). `PrequentialBenchmark` now catches both and measures such a model in parts
  (the sum of the full sizes of its sub-learners plus the shallow sizes of the objects outside them; it matches the full
  measurement within 0.01% on a smaller SGBR-FIMTDD), and notes it on stderr (`scripts/run.sh` keeps a non-empty stderr
  of a successful run as `results/logs/<dataset>.<method>.<seed>.notes`).
  These six runs are repeated with `HEAP=30g` (below 32 GB, so compressed references stay on and sizes remain
  comparable), three at a time, by `build/rerun_sgbr_fimtdd.sh` on the server, which appends to the same
  `results/raw/ensemble_parts/` files; `scripts/run_ensemble.sh` then only merges them again.

`scripts/run_all_ensembles.sh` runs tuning, selection and the ensembles unattended (e.g., in tmux), with
`TUNING_PARALLEL` and `MAIN_PARALLEL` jobs and `PYTHON` for the selection. After copying results from a server that ran
them before the class was renamed, `scripts/rename_class_in_results.sh` replaces `PLASTICRegressor` by `BRIMRegressor` in
`results/`, `configs/` and `paper/tables/` (or in the given paths; `--dry-run` only lists the files). No file is removed. Then `python3 analysis/analyze.py` also writes
`paper/tables/ensemble_*.tex` and `paper/figures/cd_ensemble_*`.

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

`results/raw/main.csv` (336 runs, baselines with tuned parameters), `results/raw/ablation.csv` (363 runs) and
`results/raw/tuning/` (2,673 runs); see `results/summary/analysis_output.txt`:

| Measure (average rank, 16 datasets) | FIMTDD | ORTO | AMRules | SGT | BRIM | BRIM-1MB | BRIM-100KB |
|---|---|---|---|---|---|---|---|
| Adjusted R² (higher is better) | 4.44 | 6.25 | 3.94 | 5.31 | **2.22** | **2.22** | 3.62 |
| Peak memory | 5.75 | 5.06 | 3.88 | 6.25 | 3.38 | 2.69 | **1.00** |
| Learner wall time | **1.94** | 2.38 | 6.25 | 6.56 | 4.12 | 3.94 | 2.81 |

Geometric mean of the peak memory relative to tuned FIMTDD: BRIM 10%, BRIM-1MB 4.7%, BRIM-100KB 0.8%; wall time: BRIM
3.1×, BRIM-100KB 1.9× (tuned FIMTDD uses mean leaves). BRIM's structural size estimate exceeded its budget in one case
(ailerons, 100 KB budget, +7% at its peak, transiently after a drift warning). On an Apple M5 Pro, tuning takes about
5 minutes and each of the main and ablation experiments about 20 minutes.

### Single trees on a server, memory at the end of the stream

`results/single_end_memory/` holds a second run of the tuning (2,673 runs) and the main experiments (336 runs) on a
10-core Xeon W-2255 (lyon, OpenJDK 21), made with `scripts/run_all_single.sh` (4 parallel tuning jobs with a 4 GB heap,
then the main runs one at a time with an 8 GB heap). Memory is measured once, at the end of each stream
(`MEMORY_SAMPLES=1`), so `mem_final_bytes` is the final model size. The selected baseline configurations and all
adjusted R² values are identical to those of `results/raw/`; wall times are about three times those on the M5 Pro.
Ensemble reruns were using other cores of the server at the same time.

| Measure (average rank, 16 datasets) | FIMTDD | ORTO | AMRules | SGT | BRIM | BRIM-1MB | BRIM-100KB |
|---|---|---|---|---|---|---|---|
| Adjusted R² (higher is better) | 4.44 | 6.25 | 3.94 | 5.31 | **2.22** | **2.22** | 3.62 |
| Final memory | 5.62 | 4.62 | 4.25 | 6.31 | 3.47 | 2.72 | **1.00** |
| Learner wall time | **2.19** | 2.62 | 6.25 | 6.44 | 4.25 | 3.88 | 2.38 |

Geometric mean of the final memory: BRIM 575 KB, BRIM-1MB 280 KB, BRIM-100KB 55 KB, FIMTDD 5.8 MB, ORTO 3.2 MB,
AMRules 1.4 MB, SGT 26.8 MB; of the wall time: BRIM 1.64 s, BRIM-100KB 1.01 s, FIMTDD 0.68 s, ORTO 0.76 s, AMRules 3.63 s,
SGT 4.97 s.

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

Base learners: FIMT-DD gets nominal attributes one-hot encoded by SGBT (it does not support nominal attributes) and keeps
SGBT's default mean leaves with a constant learning rate (`-e -p`). BRIM gets the nominal attributes as they are (`-H`;
on RTG with eager splits, 83.5% accuracy instead of 75.7% with one-hot encoding), with drift detection and its default
32 MB budget per tree. BRIM is tuned with its adaptive leaves; the final runs add mean leaves (`-e`) to the selected
configuration, since a timing test (`results/raw/sgbt_speed/`, `configs/sgbt_speed.tsv`: LED_a_100K, RBF5_f_100K and
RTG_50K) showed them 22–24% faster at the same or better accuracy (73.15 vs. 73.09, 68.54 vs. 67.75, 83.53 vs. 83.57),
whereas disabling drift detection (`-u`) saved only 4–11% and cost up to 1 point. `SGBT-BRIM-1MB`, `-500KB` and `-100KB`
limit each tree to 1 MB, 500 KB and 100 KB (`-m 1000000`, `500000`, `100000`); the budget is not tuned. A budget test
(`results/raw/sgbt_budget/`) on the same streams: 100 KB costs nothing on LED_a (73.08 vs. 73.09) but 6–7 points on RTG_50K
and RBF5_f_100K, 1 MB at most 0.9 points.

| Step | Script | Content |
|---|---|---|
| Datasets | `scripts/gen_cdatasets.sh` | Synthetic streams in `CDatasets/` (1M or 100K instances) and `CDatasets_scaled/` (`SCALE=0.1`, and 0.5 for the 100K streams), plus elecNormNew, airlines and covtypeNorm in `CDatasets/` |
| Build | `scripts/build.sh`, `scripts/build_sgbt.sh` | MOA and `PrequentialClassificationBenchmark` (`JAVAC_RELEASE=21` compiles for a Java 21 server) |
| Deploy | `scripts/deploy_sgbt.sh [user@host] [dir]` | Copies the MOA classes, the dependency jars and the SizeOf agent (`lib/`), the runner, scripts, configs and data to a server without Maven, and writes a `build/cp.txt` with the server paths |
| Tuning | `scripts/run_sgbt_tuning.sh` | 16 configurations per base learner, `-g` {10, 25, 50, 200} × `-c` {0.1, 0.05, 0.01, 1e-4} (`configs/sgbt_tuning/`), on the 11 streams of `CDatasets_scaled/` (`configs/sgbt_tuning_datasets.txt`), seed 1, no memory measurement -> `results/raw/sgbt_tuning/` |
| Selection | `analysis/select_sgbt.py` | Best average rank of accuracy; diverged or failed runs rank last. Writes `configs/sgbt_methods.tsv` (tuned SGBT-FIMTDD, tuned SGBT-BRIM, SGBT-BRIM-1MB, -500KB and -100KB, and `SGBT-default`, SGBT with its default FIMT-DD, unless tuning selects that configuration, as it did: then the runs would be identical and `SGBT-default` is left out) and `paper/tables/sgbt_tuning.tex` |
| Final runs | `scripts/run_sgbt_final.sh` | `configs/sgbt_methods.tsv` on elecNormNew, LED_100K, RBF5_100K and RTG_100K of `CDatasets/` (`configs/sgbt_datasets.txt`), seeds 1–3, memory at 10 points -> `results/raw/sgbt.csv` |
| Analysis | `analysis/analyze_sgbt.py` | Accuracy and wall time of the selected configurations on the tuning streams (`paper/tables/sgbt_tuning_streams.tex`), and accuracy, peak memory and wall time of the final runs per stream, mean ± sd over the seeds (`results/summary/sgbt_final.csv`, `paper/tables/sgbt_final.tex`). With four streams, a Friedman test and a post-hoc test over five methods are not meaningful, so there are no ranks or CD diagrams |

`scripts/run_all_sgbt.sh` runs tuning, selection and final runs unattended; on marseille.hh.se (20 cores, 125 GB, Java 21):

```bash
export SIZEOF_AGENT=/data2/home/nuwgun/brim_sgbt_classification/lib/sizeofag-1.1.0.jar \
       PYTHON=/data2/home/nuwgun/conda_envs/CondaGR/bin/python TUNING_PARALLEL=8 TUNING_HEAP=14g FINAL_PARALLEL=4 FINAL_HEAP=30g
scripts/run_all_sgbt.sh 2>&1 | tee -a results_sgbt.log
```

`scripts/run_sgbt.sh` is the classification counterpart of `run.sh` (sequential, resumable) and
`scripts/run_sgbt_parallel.sh` runs one job per (dataset, method, seed), the most expensive streams (size × classes) first.
Each run uses one core: SGBT's parallel streams are disabled (`FJ_PARALLELISM`, default 0) and the GC threads of each
JVM limited (`GC_THREADS`, default 2), so that parallel jobs do not overload the server; wall times are still affected by
the other jobs. FIMT-DD needs several GB on the multi-class streams (out of a 10 GB heap on RBF5_100K and LED_100K when
memory is measured, since the SizeOf agent needs about as much heap again as the model), hence the larger heaps. With 12 tuning jobs and 14 GB heaps, six FIMT-DD jobs on RBF5_f_100K filled marseille's 125 GB within 10 minutes; 8 jobs keep the worst case at about 76 GB.

The classification runner writes `dataset, config, seed, instances, accuracy, kappa, kappa_t, kappa_m` (percent, from
MOA's `BasicClassificationPerformanceEvaluator`), `nonfinite_votes` (instances whose votes contain NaN or infinite
values), `learner_wall_s` and `mem_peak_bytes, mem_mean_bytes, mem_final_bytes` (−1 without memory measurement).

### Failed final runs and their rerun

The first final run (27 Oct 2026, 10:33–20:26, 4 jobs with 30 GB heaps) produced 56 of 60 results. The four missing runs
are all SGBT-FIMTDD; their logs are in `results/logs/failed_final_20261027/`:

- RBF5_100K seeds 1 and 3 trained for 8.5–10 hours and then crashed in the SizeOf agent with
  `java.lang.IllegalStateException: Capacity exhausted.`: the identity map of visited objects reached its maximum
  capacity (about 2^29 objects) on the 500 FIMT-DD trees.
- RBF5_100K seed 2 (after about 2 hours) and LED_100K seed 3 (within 7 minutes) left empty logs, most likely killed by
  the Linux OOM killer: four JVMs with 30 GB heaps plus their overhead exceed marseille's 125 GB. The kernel log is not
  readable to confirm this, and the exit codes were not recorded.

Fixes: `PrequentialClassificationBenchmark` now measures the model in parts when the agent fails (out of memory or
`IllegalStateException`), as `PrequentialBenchmark` does for the regression ensembles: the full sizes of the
sub-learners (the trees) plus the shallow sizes of the objects outside them, and a note on stderr. Objects shared by the
trees (e.g., the header) are counted once per tree, a few KB each: on small SGBT models (`-g 25 -c 0.05` FIMT-DD on 5,000
instances of RBF5_100K, 855 MB; BRIM on elecNormNew, 13 MB; BRIM-100KB on LED_100K, 93 MB) the part-wise size exceeds the
whole-model size by 0.04%, 1.7% and 6.5%, i.e., by 6 KB per tree at most, which is negligible for the multi-GB FIMT-DD
models that need it. `run_sgbt.sh` now appends the JVM's exit code to the log of a failed run and prints it in the
`FAILED` line (above 128 for a death by signal, e.g., 137 = SIGKILL).

The four runs were rerun on 28 Oct 2026 (from 11:44), two at a time with 30 GB heaps, appending to the existing parts and
merging them into `results/raw/sgbt.csv` again (`configs/sgbt_rerun_methods.tsv` and `configs/sgbt_rerun_datasets.txt`;
the completed runs are skipped). Only the runner and the scripts were redeployed, so MOA is the same as for the other
runs. The paper (Section 8.2, `paper/tables/sgbt_final.tex`) reports the 56 runs of the first final run, without these
reruns: SGBT-FIMTDD is missing on RBF5_100K and has two seeds on LED_100K.

```bash
SGBT_METHODS=configs/sgbt_rerun_methods.tsv SGBT_DATASETS=configs/sgbt_rerun_datasets.txt PARALLEL=2 HEAP=30g \
  scripts/run_sgbt_final.sh 2>&1 | tee results/sgbt_rerun.log
```

Timing runs with the default configurations (seed 1, `results/raw/sgbt_smoke/`) took 0.1–1 hour per run on marseille
(LED with BRIM the longest) and showed BRIM 1–16 points behind default FIMT-DD; a probe (`results/raw/sgbt_probe.csv`) on
LED_a/g_100K and RBF5_f_100K showed that eager splitting (`-g 25 -c 0.05`) closes much of this gap (LED within 0.6 points, RBF5_f from 15.7 to 7 points) and that a 1 MB budget
per tree costs at most 0.3 points while reducing BRIM's peak memory by 29–76%.
