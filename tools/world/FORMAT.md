# Real world data format (one file per league: L<leagueId>.txt, UTF-8)

LEAGUE|<leagueId>|<season, e.g. 2026-27>|<real league name>
TEAM|<real club name>|<SHORT 3 letters>|<city>|<stadium name>|<capacity int>|<head coach full name>|<coach nation ISO2>|<#RRGGBB primary>|<#RRGGBB secondary>|<rep 40-95>
P|<shirt no, 0 if unknown>|<player name as commonly known>|<birth year>|<nation ISO2>|<DET>|<DET2 or ->|<foot R/L/B>|<ovr>|<pot>|<pac>,<sho>,<pas>,<dri>,<def>,<phy>

DET codes: KL (goalkeeper), SLB (left back / left wing-back), SGB (right back / right wing-back), STP (centre back),
DOS (defensive midfielder), MO (central midfielder), OOS (attacking midfielder / no.10), SLK (left winger), SGK (right winger), SF (striker).
Nation: ISO 3166 alpha-2 (England/Scotland/Wales/N.Ireland -> GB). Lines starting with # are comments.
Ratings use an EA FC-like 1-99 scale (own estimates). For goalkeepers the six numbers are pac,sho,pas,dri,def,phy too (ovr = goalkeeping quality).
