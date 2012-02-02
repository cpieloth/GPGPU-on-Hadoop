reset
set terminal pdf
set key left box
set grid
set title "Numerische Integration mit Hadoop & OpenCL\n(Auflösung: 100K, f(x)=xsinx)"
set output "ni_time_rc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Intervalle"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

set mxtics 2
set mytics 2
set ytics 10
#set xtics 10 nomirror

#set offset 1,1,1,1
set xrange[0:1050]
#set yrange[0:220]

plot \
'times_100000rc_cpu.csv' u ($1):($6/1000) t " CPU" w linespoints, \
'times_100000rc_ocl.csv' u ($1):($6/1000) t " GPU" w linespoints
