10 DIM A(3, 4)
20 FOR I = 1 TO 3
30  FOR J = 1 TO 4
40   A(I, J) = I * J
50  NEXT J
60 NEXT I
70 FOR I = 1 TO 3
80  FOR J = 1 TO 4
90   PRINT A(I, J); "="; I * J
100  NEXT J
110 NEXT I
120 DIM B$(1, 2, 3)
130 B$(1, 2, 3) = "ABC"
140 PRINT B$(1, 2, 3)