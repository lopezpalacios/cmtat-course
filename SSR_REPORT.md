# SSR Persona Ratings (1-5)

Method: Semantic Similarity Rating (arXiv:2510.08338) over persona free-text
reactions, local `nomic-embed-text`. Higher = better (ease: higher = easier).

## Per-chapter mean across 5 personas

| chapter | clarity | ease | relevance | engagement | trust |
|---|---|---|---|---|---|
| landing | 3.17 | 3.04 | 3.08 | 3.12 | 3.21 |
| ch01 | 3.09 | 2.97 | 3.05 | 2.97 | 3.1 |
| ch04 | 3.34 | 3.06 | 3.24 | 3.15 | 3.09 |
| ch08 | 2.97 | 2.89 | 3.01 | 2.85 | 2.95 |

## Per-persona × chapter

| persona | chapter | clarity | ease | relevance | engagement | trust |
|---|---|---|---|---|---|---|
| Arjun | landing | 3.05 | 2.88 | 3.02 | 3.21 | 3.35 |
| Arjun | ch01 | 2.96 | 2.91 | 2.89 | 3.17 | 2.92 |
| Arjun | ch04 | 3.43 | 3.33 | 3.33 | 3.28 | 3.4 |
| Arjun | ch08 | 2.78 | 2.88 | 2.96 | 2.9 | 2.77 |
| Priya | landing | 2.76 | 2.84 | 2.97 | 3.03 | 2.94 |
| Priya | ch01 | 3.19 | 3.13 | 3.21 | 2.85 | 3.16 |
| Priya | ch04 | 3.15 | 2.95 | 3.15 | 3.2 | 2.93 |
| Priya | ch08 | 2.8 | 2.82 | 2.97 | 2.78 | 2.97 |
| Daniel | landing | 3.37 | 3.09 | 3.13 | 3.12 | 3.26 |
| Daniel | ch01 | 2.93 | 2.83 | 3.01 | 2.74 | 3.18 |
| Daniel | ch04 | 3.41 | 3.02 | 3.21 | 3.0 | 3.17 |
| Daniel | ch08 | 2.97 | 2.83 | 3.01 | 2.8 | 3.04 |
| Ana | landing | 3.39 | 3.09 | 3.21 | 3.31 | 3.18 |
| Ana | ch01 | 3.3 | 2.95 | 3.04 | 2.91 | 3.13 |
| Ana | ch04 | 3.48 | 3.16 | 3.28 | 3.1 | 3.21 |
| Ana | ch08 | 3.18 | 3.01 | 3.11 | 3.15 | 3.07 |
| Milos | landing | 3.3 | 3.28 | 3.09 | 2.93 | 3.32 |
| Milos | ch01 | 3.08 | 3.05 | 3.12 | 3.18 | 3.09 |
| Milos | ch04 | 3.25 | 2.84 | 3.22 | 3.15 | 2.74 |
| Milos | ch08 | 3.12 | 2.89 | 3.0 | 2.6 | 2.89 |
