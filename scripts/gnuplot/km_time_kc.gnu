set terminal postscript eps color
set key left top box
set grid
set title "K-Means mit Hadoop & OpenCL (256 Cluster)"
set output "km_time_kc.eps"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"

set boxwidth 1.00 absolute

set style fill solid 1.00 border -1
#set style data histogram
#set style histogram cluster gap 2

#set mxtics 2
set mytics 2
#set ytics 5 nomirror
set xtics 16 nomirror

set offset 16,16,0,0
#set xrange[0:144]
set yrange[0:*]

x=1
y="6"

plot \
'times_2d_cpu.csv' u x:($6/1000) t " 2 Dimensionen (CPU) " w linespoints, \
'times_2d_ocl.csv' u x:($6/1000) t " 2 Dimensionen (GPU)" w linespoints, \
'times_64d_cpu.csv' u x:($6/1000) t " 64 Dimensionen (CPU) " w linespoints, \
'times_64d_ocl.csv' u x:($6/1000) t " 64 Dimensionen (GPU)" w linespoints, \
'times_256d_cpu.csv' u x:($6/1000) t " 256 Dimensionen (CPU) " w linespoints, \
'times_256d_ocl.csv' u x:($6/1000) t " 256 Dimensionen (GPU)" w linespoints
