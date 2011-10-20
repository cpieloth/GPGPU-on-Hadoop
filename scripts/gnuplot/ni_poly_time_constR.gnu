set terminal postscript eps color
set key left top box
set grid
set title "Numerische Integration mit Hadoop & OpenCL (100.000 Schritte, Polynom 3. Ordnung)"
set output "ni_poly_time_constR.eps"
set ylabel "Laufzeit [s]"
set xlabel "Intervalle"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

#set mxtics 2
set mytics 2
#set ytics 5 nomirror
#set xtics 16 nomirror

set offset 0,50,0,0
#set xrange[0:144]
set yrange[0:*]

x=1
y="6"

plot \
'times_100000rc_cpu.csv' u x:($6/1000) t " CPU" w linespoints, \
'times_100000rc_ocl.csv' u x:($6/1000) t " GPU" w linespoints
