# Real-Time-Aviation-Flight-Data-Analyzer
This Java program retrieves real-time aircraft state vectors from the OpenSky Network API and provides a live global overview of air traffic. It calculates key statistics such as:
- Total aircraft detected worldwide
- Number of airborne aircraft
- Aircraft over specific countries (e.g., Canada)
- Average altitude of airborne aircraft
- Top countries by number of aircraft

The program continuously refreshes at a configurable interval and handles API rate limits gracefully. It is designed for aviation enthusiasts and developers interested in real-time flight data analysis and visualization.

Features:
- Fetches live aircraft positions using OpenSky API
- Processes JSON responses with Jackson
- Displays global statistics in the console
- Handles API errors and rate limiting
- Clear console output for a “live” dashboard feel

Version Information:
- Version 1: Global tracker using live OpenSky API data (restricted for free accounts)
- Version 2: Tracks airborne flights within 50 km of YVR, shows distance, altitude and callsign
  
Note:
Requires an OpenSky account with verified API access.
Free accounts have restrictions on global live data; regional or pre-curated datasets are recommended for personal projects.
