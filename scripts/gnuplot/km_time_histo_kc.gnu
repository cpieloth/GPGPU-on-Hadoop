reset
set terminal pdf
set key under nobox
set grid y
#set output "km_time_histo_kc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"
set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram
set xrange [-1:11]
set mytics 2
set xtics nomirror

map="Map"
misc="Hadoop"
reduce="Reduce"

set title "K-Means mit Hadoop & OpenCL - 256 Cluster & 2 Dimensionen"
set output "km_time_histo_2d_kc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_2d_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_2d_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set xrange [0:13]
set title "K-Means mit Hadoop & OpenCL - 256 Cluster & 64 Dimensionen"
set output "km_time_histo_64d_kc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_64d_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_64d_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set title "K-Means mit Hadoop & OpenCL - 256 Cluster & 256 Dimensionen"
set output "km_time_histo_256d_kc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_256d_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_256d_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle
