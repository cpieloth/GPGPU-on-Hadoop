reset
set terminal pdf
set key left box
set grid
set title "Numerische Integration mit Hadoop & OpenCL\n(1000 Intervalle, f(x)=xsinx)"
set output "ni_time_ic.pdf
set ylabel "Laufzeit [s]"
set xlabel "Auflösung [Kilo]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

set mxtics 2
set mytics 2
set ytics 25
#set xtics 16 nomirror

#set offset 1,1,1,1
set xrange[0:1050]
set yrange[0:220]

plot \
'times_1000ic_cpu.csv' u ($1/1000):($6/1000) t " CPU" w linespoints, \
'times_1000ic_ocl.csv' u ($1/1000):($6/1000) t " GPU" w linespoints
