reset
set terminal pdf
set key under nobox
set grid y
#set output "km_time_histo_dc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"
set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram
set xrange [0:13]
set mytics 2
set xtics nomirror
map="Map"
misc="Hadoop"
reduce="Reduce"

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 64 Cluster"
set output "km_time_histo_64k_dc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_64k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_64k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 256 Cluster"
set output "km_time_histo_256k_dc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_256k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_256k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 1024 Cluster"
set output "km_time_histo_1024_dc.pdf"
plot \
newhistogram "CPU" lt 1, 'times_1024k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_1024k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle
