-- name: dbspec-parameters-query
SELECT CURRENT_TIMESTAMP AS time
FROM SYSIBM.SYSDUMMY1
WHERE :value1 = 1
AND :value2 = 2
AND ? = 3
AND :value2 = 2
AND ? = 4
