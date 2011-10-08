:: Numerical Integration ::
using trapezoidal rule

mapper: read in data -> identifier, integral
reduce: calculation on CPU or GPU -> number of reduce task must be specified

mapred-site.xml
	<property>
		<name>mapred.reduce.tasks</name>
		<value><!-- 2x nodes --></value>
	</property>
	
Create specified paths from runtime.py!