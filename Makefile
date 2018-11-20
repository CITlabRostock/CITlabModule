ci clean_install:
	mvn clean install
	
ciwt clean_install_wo_tests:
	mvn -Dmaven.test.skip=true clean install