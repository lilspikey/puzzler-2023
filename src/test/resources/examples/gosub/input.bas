10 PRINT "BEFORE SUB1"
20 GOSUB 80
30 PRINT "AFTER SUB1"
40 PRINT "BEFORE SUB2"
50 GOSUB 100
60 PRINT "AFTER SUB2"
70 END
80 PRINT "INSIDE SUB1"
90 RETURN
100 PRINT "INSIDE SUB2"
110 PRINT "BEFORE SUB3"
120 GOSUB 150
130 PRINT "AFTER SUB3"
140 RETURN
150 PRINT "INSIDE SUB3"
160 RETURN
