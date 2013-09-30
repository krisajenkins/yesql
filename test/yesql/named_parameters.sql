-- Here's a query with some named and some anonymous parameters.
-- (...and some repeats.)
SELECT CURRENT_TIMESTAMP AS time
FROM SYSIBM.SYSDUMMY1
WHERE :value1 > 10
AND :value2 > 20
AND ? < 50
AND :value2 < 100
