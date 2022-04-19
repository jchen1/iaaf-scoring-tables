# iaaf-scoring-tables

Parses [World Athletics Scoring Tables](https://www.worldathletics.org/about-iaaf/documents/technical-information) PDFs. Outputs include:
- `iaaf.sqlite`: Raw scores in SQLite format. Contains exactly one table, `points`, with schema
  ```sql
    id INTEGER PRIMARY KEY,
    category TEXT NOT NULL, // indoor, outdoor
    gender TEXT NOT NULL, // men, women
    event TEXT NOT NULL,
    mark REAL NOT NULL,
    points INTEGER NOT NULL,
    UNIQUE(category, gender, event, mark),
    UNIQUE(category, gender, event, points)  
  ```
- `iaaf.json`: The same data as above, stored as a flat JSON file
- `coefficients.json`: Quadratic coefficients for each category/gender/event tuple, inspired by https://sports.stackexchange.com/questions/15533/how-to-calculate-iaaf-points