       

-- name: the-time
-- This is another time query.
-- Exciting, huh?
SELECT CURRENT_TIMESTAMP
FROM SYSIBM.SYSDUMMY1
  
-- name: sums
-- Just in case you've forgotten
-- I made you a sum.
SELECT
    :a + 1 adder,
    :b - 1 subtractor
FROM SYSIBM.SYSDUMMY1

-- name: edge
-- And here's an edge case.
-- Comments in the middle of the query.
SELECT
    1 + 1 AS two
    -- I find this query dull.
FROM SYSIBM.SYSDUMMY1
