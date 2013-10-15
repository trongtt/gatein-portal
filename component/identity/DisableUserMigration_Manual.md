# "Disable_User" database migration script

The "disable user" feature implement in GTNPORTAL-3227. The GateIn's picketlink organization service implementation need legacy database to be migrated (Look at Jira issue for details)
To migrate data, just run this java class: 
_org.exoplatform.services.organization.idm.DisabledUserMigrationScript_ (located in exo.portal.component.identity-xxx.jar)

# Quickstart :
Default configuration works for default GateIn configuration (hsqldb organization database, default picketlink realm)

_java -cp lib/*:target/exo.portal.component.identity-xxx.jar -Dhibernate.connection.url=[connection_url] _org.exoplatform.services.organization.idm.DisabledUserMigrationScript_

# Configuration

This is standalone java application, it can be run with-out portal container. User can run it by Eclipse: choose option "Run as Java Application" (need to modify hibernate.connection.url in configuration.properties) or use java command:

_java -cp lib/*:target/exo.portal.component.identity-xxx.jar [systemProperties] org.exoplatform.services.organization.idm.DisabledUserMigrationScript [config.properties]_

## Library: those libs should be in classpath

lib/*  : contains hiberate, picketlink and log4j library: 
	antlr-2.7.6rc1.jar
	hsqldb-2.0.0.jar
	picketlink-idm-common-1.4.3.Final.jar
	dom4j-1.6.1.jar
	javassist-3.14.0-GA.jar
	picketlink-idm-core-1.4.3.Final.jar
	exo.kernel.commons-2.5.0-Alpha1.jar
	jboss-logging-3.1.2.GA.jar
	picketlink-idm-hibernate-1.4.3.Final.jar
	hibernate-commons-annotations-4.0.1.Final.jar
	jta-1.1.jar
	picketlink-idm-spi-1.4.3.Final.jar
	hibernate-core-4.1.6.Final.jar
	log4j-1.2.16.jar
	slf4j-api-1.6.1.jar
	hibernate-jpa-2.0-api-1.0.1.Final.jar
	picketlink-idm-api-1.4.3.Final.jar  slf4j-simple-1.6.1.jar

## Properties

[systemProperties] : customize the migration behaviour. All properties are optional, there is a default configuration.properties contains default configurations for the script:

_picketlink.config_file_path=picketlink-idm-config.xml_           --> path to picketlink config file, this file must be in the classpath

_picketlink.realmName=idm_realm_portal_		                --> picketlink realm name

_hibernate.connection.driver_class=org.hsqldb.jdbcDriver_

_hibernate.connection.url=jdbc:hsqldb:file:path/to/file_ 

_hibernate.connection.username=sa_

_hibernate.connection.password=password_

_hibernate.dialect=org.hibernate.dialect.HSQLDialect_

_hibernate.config_path=hibernate.cfg.xml_		                --> DB connection infos are set by above configs, but if user want more hibernate config, just specify path to hibernate config file, this file must be in the classpath

_enable_user_from=0_						        --> script will start to migrate users from this index (user is arrange by name in ASCENDING order)

_batch=100_								        --> by default, it migrate for a batch 100 users in a transaction, if there is any problem, the transaction will be rolled-back for that 100 users, look at the console, there is a log to show it's fail from which index, then you can use "enable_user_from" optioni to run again from that index. This should be usefull for large database

If there is no [systemProperties]. Migration script will be configured by a property file, that is specify by [config.properties] argument (this properties file must be in the classpath)
The default configuration files are in the JAR file (configuration.properties, hibernate.cfg.xml, picketlink-idm-config.xml). They will be use in case there is no system properties or argument.

# Example: How to customize migration behaviour

Let say we have custom properties file: _conf/config.properties_. This file contains those configs
_picketlink.config_file_path=idm-config.xml_    --> custom config for PicketlinkIDM

_picketlink.realmName=idm_realm_portal_

_hibernate.config_path=hi.cfg.xml_		      --> custom config for Hibernate, this file should contains DB connection infos

_hibernate.dialect=org.hibernate.dialect.HSQLDialect_

_batch=1000_

All the config file should be in the classpath, for example, if they are in the directory "conf", run this command:

_java -cp conf:lib/*:target/exo.portal.component.identity-xxx.jar -Denable_user_from=100 org.exoplatform.services.organization.idm.DisabledUserMigrationScript config.properties_

--> custom config files will be loaded, and it will start to migrate users from index: 100
