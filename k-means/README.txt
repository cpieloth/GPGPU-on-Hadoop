::: K-Means-Clustering :::

limitations:
	- Cluster are shared via distributed cache
	- Cluster must fit in RAM (CPU and GPU)!

mapper: OCL or CPU (single)
reduce: CPU (single)

mapred-site.xml
	<property>
		<name>mapred.reduce.tasks</name>
		<value><!-- 2x nodes --></value>
	</property>
	
NIHadoopNamed.jar