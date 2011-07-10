set terminal postscript eps color
set key left top box

set title "MaxTemperature mit Hadoop & OpenCL"
set output "maxTemp.eps"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
set style data histogram
set style histogram cluster gap 2

set mxtics 1
set mytics 1
set ytics 5 nomirror
set xtics 1 nomirror

#set xrange[32:*]
#set yrange[0:30]

plot \
'hadoop.dat' using 2:xtic(1) t "Hadoop", \
'javacl.dat' using 2 t "JavaCL", \
'jocl.dat' using 2 t "JOCL", \
'streaming.dat' using 2 t "Streaming", \
'pipes.dat' using 2 t "Pipes"
