-- It's the time query again, but there's an inline comment to make the parser fret.
SELECT
 CURRENT_TIMESTAMP AS time, -- Here is an inline comment.
 'Not -- a comment' AS string
FROM SYSIBM.SYSDUMMY1
