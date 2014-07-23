-- name: this-has-trailing-whitespace    
-- The name has trailing whitespace here ^^^^
-- But the parser should not blow up.
SELECT CURRENT_TIMESTAMP
FROM SYSIBM.SYSDUMMY1
