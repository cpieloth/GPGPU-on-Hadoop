reset
set terminal pdf
set key under nobox
set grid
set title "K-Means mit Hadoop & OpenCL (64 Dimensionen)"
set output "km_time_dc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

#set mxtics 2
set mytics 2
set ytics nomirror
set xtics nomirror

set offset 0,0,0,0
set xrange[0:144]
set yrange[0:*]

x=1
y=6

plot \
'times_64k_cpu.csv' u x:($6/1000) t "64 Cluster (CPU) " w linespoints, \
'times_64k_ocl.csv' u x:($6/1000) t "64 Cluster (GPU)" w linespoints, \
'times_256k_cpu.csv' u x:($6/1000) t "256 Cluster (CPU) " w linespoints, \
'times_256k_ocl.csv' u x:($6/1000) t "256 Cluster (GPU)" w linespoints, \
'times_1024k_cpu.csv' u x:($6/1000) t "1024 Cluster (CPU) " w linespoints, \
'times_1024k_ocl.csv' u x:($6/1000) t "1024 Cluster (GPU)" w linespoints
