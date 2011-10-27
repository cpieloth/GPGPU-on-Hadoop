set terminal pdf
set key right nobox
set grid
set output "ni_poly_time_histo_ic.pdf"
set ylabel "Laufzeit [s]"
set xlabel "Aufloesung [Kilo]"

set boxwidth 0.8 absolute
set style fill solid 2 border lt -1
set style histogram rowstacked gap 1
set style data histogram
set xtic rotate by -45

map="map"
misc="init, sort"
reduce="reduce"

set title "Numerische Integration mit Hadoop & OpenCL (500 Intervalle, Polynom 3. Ordnung)"
plot \
newhistogram "CPU" lt 1, 'times_500ic_cpu.csv' u ($3/1000):xtic(sprintf("%d", $1/1000)) t map, '' u (($6-$3-$5)/1000) t misc, '' u ($5/1000) t reduce, \
newhistogram "GPU" lt 1, 'times_500ic_ocl.csv' u ($3/1000):xtic(sprintf("%d", $1/1000)) notitle, '' u (($6-$3-$5)/1000) notitle, '' u ($5/1000) notitle
