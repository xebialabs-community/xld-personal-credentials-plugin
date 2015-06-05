# Personal Credentials plugin #

# Overview #

The personal credentials plugin allows you to specify overthere.Host credentials that are used only for a particular deployment.

The following features are available : 

- specify a username/password couple (CredentialsType.CT_USERNAME)
- specify a username/password for Windows hosts and a username/password for Unix hosts (CredentialsType.CT_USERNAME and windows/unix properties)
- specify a private key file path and a passphrase (CredentialsType.CT_PRIVATEKEY)
- specify a private key selector (an alias) and a passphrase (CredentialsType.CT_PRIVATEKEY_SELECTOR) 
- specify a custom python script to set the credentials on hosts in a custom way (CredentialsType.CT_CUSTOM_SCRIPT)
- optionally insert a check connection step for each hosts involved in the deployment plan

# Requirements #

* **XLDeploy requirements**
	* **XLDeploy**: version 4.5.2+, 5.0.0+

# Installation #

Place the plugin JAR file into your `SERVER_HOME/plugins` directory.

# Configuration #

The activation of the personal credential is triggered per environment using the _overrideHostCredentials_ property.

```
	<type-modification type="udm.Environment">
        <property name="overrideHostCredentials" kind="boolean" default="false" category="Personal Credentials"/>
	</type-modification>
```
	
## Single credential - CT_USERNAME ##

This configuration allows the user to supply a username and a password on the deployed application level. These credentials will be used each time XLDeploy needs to create a new remote connection to a host during the execution of the deployment plan.

Enable single personal credentials by adding the following in the synthetic.xml:

```
	<type-modification type="udm.DeployedApplication">
        <property name="username" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="password" password="true" transient="true" required="false" category="Personal Credentials"/>
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_USERNAME" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials" hidden="true"/>
    </type-modification>
```    

## Per OS credentials - CT_USERNAME and windows/unix properties ##

This configuration allows the user to supply a username and a password on the deployed application level.
These credentials will be used each time XLDeploy needs to create a new remote connection during the execution of the deployment plan.
If the host operating system is *Windows*, _windowsUsername_ and _windowsPassword_ will be used for username and password.
If the host operating system is *Unix*, _unixUsername_ and _unixPassword_ will be used for username and password.

To enable per OS credentials, modify your synthetic.xml file in the following way:

```
<type-modification type="udm.DeployedApplication">
        <property name="unixUsername" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="unixPassword" password="true" transient="true" required="false" category="Personal Credentials"/>
        <property name="windowsUsername" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="windowsPassword" password="true" transient="true" required="false" category="Personal Credentials"/>
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_USERNAME" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials" hidden="true"/>
    </type-modification>
```



## Private key credentials - CT_PRIVATEKEY ##

In this configuration the plugin will set the privatekeyfile and the passphrase property of the concerned overthere.Host(s).
To enable this configuration use the following definition :


```	
<type-modification type="udm.DeployedApplication">
		<property name="privateKey" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="passphrase" kind="string" transient="true" password="true" required="false" category="Personal Credentials"/>
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_PRIVATEKEY" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials" hidden="true"/>
    </type-modification>
```


## Private key credentials with file lookup - CT_PRIVATEKEY_SELECTOR ##

In private key selector mode, the privatekey property will be used as a key to perform a lookup against a configuration CI

```
	<type-modification type="udm.DeployedApplication">
		<property name="privateKey" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="passphrase" kind="string" transient="true" password="true" required="false" category="Personal Credentials"/>
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_PRIVATEKEY" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials" hidden="true"/>
    </type-modification>
```



## Custom script - CT_CUSTOM_SCRIPT ##

The custom script mode is intended for use cases where you want to perform a lookup of credentials in a specific way (3rd party password storage, internal repository of credentials, etc...). 
The custom script does not allow you to return custom steps, it is simply a hook where you can access the involved hosts and perform the ad'hoc credential configuration with a custom logic.
```
	<type-modification type="udm.DeployedApplication">
        <property name="scriptPath" kind="string" hidden="true" required="true" category="Personal Credentials" default="xlc/personalcredentials/setcredentials.py"/>
        <property name="scriptClasspath" kind="string" hidden="true" category="Personal Credentials" default=""/>        
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_CUSTOM_SCRIPT" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials" hidden="true"/>
    </type-modification>
```


Example Python script
``` 
logger.info("DeployedApplication id %s"%deployedApplication.id)
logger.info("Environment id %s"%environment)
for h in hosts:
	logger.info("Hosts used by deployment %s (%s)"%(h.name, h.getProperty("address")))
	#perform any credentials lookup logic here
	h.setProperty("username","someUserName")
	h.setProperty("password","somePazzW0rd")
logger.info("done")	
```
The following variables are injected to the script:

- logger  : an org.slf4j.Logger
- deployedApplication : a com.xebialabs.deployit.plugin.api.udm.DeployedApplication
- environment : a com.xebialabs.deployit.plugin.api.udm.Environment
- hosts : a Set<com.xebialabs.deployit.plugin.overthere.Host>

The script can be embedded in your plugin build (simply adapt the /xlc/personalcredentials/setcredentials.py) or overriden through the use of the /ext/ directory. The following properties allow the configuration of your script:

```
		<property name="scriptPath" kind="string" hidden="true" required="true" category="Personal Credentials" default="xlc/personalcredentials/setcredentials.py"/>
        <property name="scriptClasspath" kind="string" hidden="false" category="Personal Credentials" default=""/>
```

The scriptClasspath property allows for additional script definition, if you want to use multiple .py files (for function definitions) : it must contains 1 or more script path separated by ":", whatever the OS running XLDeploy.
Example :

```
	<property name="scriptClasspath" kind="string" required="false"  hidden="true" category="Personal Credentials" default="xlc/personalcredentials/library.py:xlc/personalcredentials/library2.py"/>
```


## Enabling all features ## 

To enable all features and select the strategy on the fly, use the following definition or adjust for your own needs:
```
 	<type-modification type="udm.DeployedApplication">
        <property name="unixUsername" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="unixPassword" password="true" transient="true" required="false" category="Personal Credentials"/>
        <property name="windowsUsername" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="windowsPassword" password="true" transient="true" required="false" category="Personal Credentials"/>
        <property name="checkConnection" kind="boolean" default="true" required="false" category="Personal Credentials" />
        <property name="credentialsType" kind="enum" default="CT_USERNAME" enum-class="ext.deployit.community.plugin.personalcredentials.ci.CredentialsType" required="false" category="Personal Credentials"/>
        <property name="privateKey" kind="string" transient="true" required="false" category="Personal Credentials"/>
        <property name="passphrase" kind="string" transient="true" password="true" required="false" category="Personal Credentials"/>
        <property name="scriptPath" kind="string" hidden="true" required="true" category="Personal Credentials" default="xlc/personalcredentials/setcredentials.py"/>
        <property name="scriptClasspath" kind="string" hidden="true" required="false" category="Personal Credentials" default=""/>
    </type-modification>
```


## Notes ##
- The 'checkConnection' property allows to generate CheckConnection Step on all the hosts involved in the personal-credentials process.
- The transient attribute equals 'true' implies the values will not be persisted after the deployment. If you want to make it persistent, set the transient attribute value to 'false'.
- Use _gradlew clean build_ for gradle build
