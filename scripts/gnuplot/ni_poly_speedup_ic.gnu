reset
set terminal pdf
set key left top box
set grid
set title "Numerische Integration mit Hadoop & OpenCL\n(500 Intervalle, Polynom 3. Ordnung)"
set output "ni_speedup_ic.pdf"
set ylabel "Speedup"
set xlabel "Auflösung [Kilo]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

#set mxtics 2
set mytics 2
#set ytics 0.5 nomirror
#set xtics 16 nomirror

#set offset 1,1,0,0
set xrange[0:1050]
set yrange[0.5:*]

plot \
'times_500ic.csv' u ($1/1000):($6/$11) t "CPU/GPU" w linespoints
