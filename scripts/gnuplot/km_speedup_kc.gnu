reset
set terminal pdf
set key left top box
set grid
set title "k-Means mit Hadoop & OpenCL (256 Gruppen)"
set output "km_speedup_kc.pdf"
set ylabel "Speedup"
set xlabel "Daten [MB]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

#set mxtics 2
set mytics 2
set ytics 0.5# nomirror
set xtics 16# nomirror

set offset 16,16,0,0
#set xrange[0:144]
set yrange[0.5:3]

x=1
y="6"

plot \
'times_2d.csv' u x:($6/$11) t " 2 Dimensionen " w linespoints, \
'times_64d.csv' u x:($6/$11) t " 64 Dimensionen " w linespoints, \
'times_256d.csv' u x:($6/$11) t " 256 Dimensionen " w linespoints
