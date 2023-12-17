10 A = 2
20 B = 0
20 FOR N = 0 TO 3
30 IF A > N THEN PRINT "A > " ; N : B = A
40 NEXT
50 IF B = A THEN 70
60 PRINT "B <> A"
70 PRINT "B = A"
80 A$ = "hello"
90 B$ = "hi"
100 IF A$=B$ THEN PRINT "A$ = B$"
110 B$ = A$
120 IF A$=B$ THEN PRINT "A$ = B$"