set terminal pdf
set key outside right top nobox
set grid
set output "km_time_histo_dc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Daten [MB]"

set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram

map="map"
misc="init, sort"
reduce="reduce"

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 64 Cluster"
plot \
newhistogram "CPU" lt 1, 'times_64k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_64k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 256 Cluster"
plot \
newhistogram "CPU" lt 1, 'times_256k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_256k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

set title "K-Means mit Hadoop & OpenCL - 64 Dimensionen & 1024 Cluster"
plot \
newhistogram "CPU" lt 1, 'times_1024k_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_1024k_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle
