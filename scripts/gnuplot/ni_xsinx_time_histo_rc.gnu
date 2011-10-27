set terminal pdf
set key right nobox
set grid
set output "ni_xsinx_time_histo_rc.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Intervalle"

set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram
set xtic rotate by -45

map="map"
misc="init, sort"
reduce="reduce"

set title "Numerische Integration mit Hadoop & OpenCL (100.000 Aufloesung, f(x)=xsinx)"
plot \
newhistogram "CPU" lt 1, 'times_100000rc_cpu.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_100000rc_ocl.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle
