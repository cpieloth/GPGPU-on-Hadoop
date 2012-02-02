reset
set terminal pdf
set key under nobox
set grid y
#set output "ni_time_histo_ic.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Auflösung [Kilo]"
set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram
set xrange [0:*]
set mytics 2
set xtics nomirror rotate by -45 scale 0

map="Map"
misc="Hadoop"
reduce="Reduce"

set title "Numerische Integration mit Hadoop & OpenCL\n(1000 Intervalle, f(x)=xsinx)"
set output "ni_time_histo_ic.pdf"
plot \
newhistogram "CPU" lt 1, 'times_1000ic_cpu_kilo.csv' u ($3/1000):xtic(1) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_1000ic_ocl_kilo.csv' u ($3/1000):xtic(1) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle

