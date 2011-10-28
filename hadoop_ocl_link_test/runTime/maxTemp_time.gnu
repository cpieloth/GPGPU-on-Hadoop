set terminal pdf
set key outside right top nobox

set title "MaxTemperature mit Hadoop & OpenCL"
set output "maxTemp.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"

set boxwidth 0.9 absolute

set style fill solid 1.00 border -1
set style data histogram
set style histogram cluster gap 2

set mxtics 0
set mytics 2
set ytics 10 nomirror
set xtics 0 nomirror
set grid y

set xrange[-0.5:2.6]
set yrange[0:*]

plot \
'hadoop.dat' u 2:xtic(1) t "Hadoop", \
'javacl.dat' u 2 t "JavaCL", \
'jocl.dat' u 2 t "JOCL", \
'streaming.dat' u 2 t "Streaming", \
'pipes.dat' u 2 t "Pipes"
