10 PRINT "Default step"
20 FOR I = 0 TO 6
30 PRINT I
40 NEXT I
50 PRINT "Step 2"
60 FOR I = 0 TO 6 STEP 2
70 PRINT I
80 NEXT I
90 PRINT "Different start and end"
100 FOR I = 1 TO 3
110 PRINT I
120 NEXT I
130 PRINT "Nested loop"
140 FOR I = 1 TO 3
150  FOR J = 2 TO 4
160   PRINT I " " J
170  NEXT J
180 NEXT I